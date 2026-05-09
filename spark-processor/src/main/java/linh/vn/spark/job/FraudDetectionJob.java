package linh.vn.spark.job;

import linh.vn.spark.config.SparkConfig;
import linh.vn.spark.model.FraudAlert;
import linh.vn.spark.model.PaymentEvent;
import linh.vn.spark.processor.PatternDetector;
import linh.vn.spark.processor.ZScoreWindowCalculator;
import linh.vn.spark.sink.HBaseSink;
import linh.vn.spark.sink.KafkaSink;
import linh.vn.spark.sink.RedisSink;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.VoidFunction2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * ═══════════════════════════════════════════════════════════════════
 * FraudDetectionJob — Spark Structured Streaming
 * ═══════════════════════════════════════════════════════════════════
 * <p>
 * Pipeline:
 * Kafka [payment-events]
 * ↓ parse JSON
 * ↓ watermark (handle late data)
 * ↓ window groupBy(userId, 1h sliding / 5min slide)
 * ↓ foreachBatch:
 * - PatternDetector.detect() → smurfing, high-freq
 * - ZScoreWindowCalculator.check() → deep Z-Score từ HBase
 * → KafkaSink → [anomaly-events]
 * → HBaseSink → [fraud_logs]
 * → RedisSink → [fraud_count dashboard]
 * <p>
 * Submit command:
 * spark-submit \
 * --class linh.vn.spark.job.FraudDetectionJob \
 * --master spark://spark-master:7077 \
 * spark-processor-shaded.jar \
 * kafka:9092 localhost 2181 localhost 6379
 * <p>
 * Args: [bootstrapServers] [hbaseQuorum] [hbasePort] [redisHost] [redisPort]
 * ═══════════════════════════════════════════════════════════════════
 */
@Slf4j
public class FraudDetectionJob {

    public static void main(String[] args) throws Exception {

        // ── Config từ args (dễ override khi deploy) ───────────────────────────
        String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
        String hbaseQuorum = args.length > 1 ? args[1] : "localhost";
        String hbasePort = args.length > 2 ? args[2] : "2181";
        String redisHost = args.length > 3 ? args[3] : "localhost";
        int redisPort = args.length > 4 ? Integer.parseInt(args[4]) : 6379;

        log.info("Starting FraudDetectionJob | kafka={} hbase={}:{} redis={}:{}",
                bootstrapServers, hbaseQuorum, hbasePort, redisHost, redisPort);

        // ── Khởi tạo Sinks (Serializable, gửi đến executor) ──────────────────
        HBaseSink hbaseSink = new HBaseSink(hbaseQuorum, hbasePort);
        KafkaSink kafkaSink = new KafkaSink(bootstrapServers);
        RedisSink redisSink = new RedisSink(redisHost, redisPort);
        PatternDetector patternDetector = new PatternDetector();

        // ── SparkSession ──────────────────────────────────────────────────────
        SparkSession spark = SparkConfig.createSession(
                "CineGoTicket-FraudDetection",
                "local[*]"  // đổi thành "spark://spark-master:7077" khi deploy cluster
        );

        // ── Schema của PaymentEvent JSON ──────────────────────────────────────
        // Định nghĩa schema giúp Spark nhanh hơn (không cần infer) và type-safe
        StructType paymentSchema = new StructType()
                .add("paymentId", DataTypes.StringType)
                .add("userId", DataTypes.StringType)
                .add("movieId", DataTypes.StringType)
                .add("amount", DataTypes.DoubleType)
                .add("status", DataTypes.StringType)
                .add("deviceIp", DataTypes.StringType)
                .add("time", DataTypes.LongType)     // epoch milli
                .add("location", DataTypes.StringType)
                .add("returnCode", DataTypes.StringType);

        // ── Đọc từ Kafka ─────────────────────────────────────────────────────
        Dataset<Row> kafkaStream = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", bootstrapServers)
                .option("subscribe", "payment-events")
                // "earliest" để không bỏ sót message khi job restart
                .option("startingOffsets", "earliest")
                // Giới hạn số message mỗi trigger để tránh OOM
                .option("maxOffsetsPerTrigger", 10000)
                .load();

        // ── Parse JSON value ──────────────────────────────────────────────────
        Dataset<Row> parsed = kafkaStream
                .select(
                        // Kafka message: key (userId) + value (JSON PaymentEvent)
                        col("key").cast(DataTypes.StringType).alias("kafkaKey"),
                        from_json(
                                col("value").cast(DataTypes.StringType),
                                paymentSchema
                        ).alias("data"),
                        col("timestamp").alias("kafkaTimestamp")
                )
                .select(
                        col("data.*"),
                        col("kafkaTimestamp"),
                        // Convert epoch milli → Timestamp để dùng với Spark window functions, ❌ if, ?: → không dùng được; ✅ phải dùng: when / otherwise
                        when(col("data.time").isNotNull(),
                                col("data.time").divide(1000)
                        ).otherwise(
                                unix_timestamp(col("kafkaTimestamp"))
                        ).cast(DataTypes.TimestampType).alias("eventTime")
                )
                // Bỏ qua rows parse lỗi (null data)
                .filter(col("data").isNotNull())
                .filter(col("userId").isNotNull());

        // ── Watermark: handle late-arriving data (chấp nhận trễ tối đa 10 phút) ──
        // Nếu event đến trễ hơn 10 phút so với watermark → bị discard
        // Watermark cần thiết để Spark biết khi nào "close" window
        Dataset<Row> withWatermark = parsed
                .withWatermark("eventTime", "10 minutes");

        // ── Window Aggregation: group theo userId trong sliding window 1h ─────
        // Window: 1 giờ, slide mỗi 5 phút
        // → Mỗi 5 phút Spark tính toán lại tất cả events trong 1h gần nhất
        // Đây là SLIDING WINDOW — khác với TUMBLING window (không overlap)
        Dataset<Row> windowed = withWatermark
                .groupBy(
                        col("userId"),
                        window(col("eventTime"), "1 hour", "5 minutes")
                )
                // Collect tất cả events của user trong window để PatternDetector phân tích
                .agg(
                        collect_list(struct(
                                col("paymentId"),
                                col("amount"),
                                col("status"),
                                col("deviceIp"),
                                col("movieId"),
                                col("time"),
                                col("returnCode")
                        )).alias("events"),
                        count("*").alias("eventCount"),
                        sum("amount").alias("totalAmount"),
                        avg("amount").alias("avgAmount")
                );

        // ── foreachBatch: xử lý mỗi micro-batch ──────────────────────────────
        // foreachBatch cho phép dùng arbitrary code (không giới hạn trong Spark API)
        // Perfect cho việc ghi vào HBase, Redis, Kafka với custom logic
        VoidFunction2<Dataset<Row>, Long> batchProcessor = (batchDf, batchId) -> {
            log.info("[FraudDetectionJob] Processing batch #{}", batchId);

            // Collect về driver (acceptable vì mỗi batch có watermark giới hạn)
            // Trong production với scale lớn: dùng foreachPartition thay vì collect
            List<Row> rows = batchDf.collectAsList();

            if (rows.isEmpty()) {
                log.info("[FraudDetectionJob] Batch #{} is empty, skipping", batchId);
                return;
            }

            List<FraudAlert> allAlerts = new ArrayList<>();
            String today = LocalDate.now().toString();

            for (Row row : rows) {
                String userId = row.getAs("userId");
                long eventCount = row.getAs("eventCount");

                // Convert Spark Row list → PaymentEvent list
                List<Row> eventRows = row.getList(row.fieldIndex("events"));
                List<PaymentEvent> events = rowsToPaymentEvents(userId, eventRows);

                // ── Pattern Detection (smurfing, high-frequency) ──────────────
                List<FraudAlert> patternAlerts = patternDetector.detect(userId, events);
                allAlerts.addAll(patternAlerts);

                // ── Deep Z-Score từ HBase ─────────────────────────────────────
                // Chỉ check Z-Score cho event lớn nhất trong window (tránh gọi HBase nhiều lần)
                events.stream()
                        .max((a, b) -> Double.compare(a.getAmount(), b.getAmount()))
                        .ifPresent(maxEvent -> {
                            ZScoreWindowCalculator zscoreCalc = new ZScoreWindowCalculator(hbaseSink);
                            FraudAlert zAlert = zscoreCalc.check(maxEvent);
                            if (zAlert != null) allAlerts.add(zAlert);
                        });

                log.info("[FraudDetectionJob] User={} events={} alerts={}",
                        userId, eventCount, allAlerts.size());
            }

            // ── Sink: publish và lưu tất cả alerts ───────────────────────────
            if (!allAlerts.isEmpty()) {
                // 1. Kafka → downstream consumers (notification, block-user service)
                kafkaSink.publishFraudAlerts(allAlerts);

                // 2. HBase → long-term storage, audit, ML training data
                hbaseSink.writeFraudAlerts(allAlerts);

                // 3. Redis → dashboard counter
                allAlerts.forEach(a -> redisSink.incrementFraudCount(today));

                log.warn("[FraudDetectionJob] Batch #{}: {} fraud alerts detected and published",
                        batchId, allAlerts.size());
            }
        };

        // ── Start streaming query ─────────────────────────────────────────────
        StreamingQuery query = windowed
                .writeStream()
                .outputMode("update")  // chỉ output rows có thay đổi (efficient hơn "complete")
//                .trigger(Trigger.ProcessingTime(String.valueOf(Duration.ofSeconds(30)))) // micro-batch mỗi 30s
                .trigger(Trigger.ProcessingTime("30 seconds"))
                .foreachBatch(batchProcessor)
                .option("checkpointLocation", "/tmp/spark-checkpoint/fraud-detection")
                .start();

        log.info("[FraudDetectionJob] Streaming query started. Waiting for termination...");
        query.awaitTermination();
    }

    /**
     * Convert Spark Row structs → PaymentEvent objects.
     */
    private static List<PaymentEvent> rowsToPaymentEvents(String userId, List<Row> rows) {
        List<PaymentEvent> events = new ArrayList<>();
        for (Row r : rows) {
            PaymentEvent e = new PaymentEvent();
            e.setUserId(userId);
            e.setPaymentId(r.getAs("paymentId"));
            e.setAmount(r.<Double>getAs("amount"));
            e.setStatus(r.getAs("status"));
            e.setDeviceIp(r.getAs("deviceIp"));
            e.setMovieId(r.getAs("movieId"));
            e.setReturnCode(r.getAs("returnCode"));
            Long time = r.getAs("time");
            e.setTime(time);
            events.add(e);
        }
        return events;
    }
}