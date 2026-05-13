package linh.vn.spark.job;

import linh.vn.spark.config.SparkConfig;
import linh.vn.spark.sink.HBaseSink;
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
import java.util.List;

import static org.apache.spark.sql.functions.*;

/**
 * ═══════════════════════════════════════════════════════════════════
 * AnalyticsJob — Spark Structured Streaming
 * ═══════════════════════════════════════════════════════════════════
 * <p>
 * Trách nhiệm: window-based analytics, KHÔNG làm fraud detection.
 * Chạy song song với FraudDetectionJob — cùng subscribe "payment-events"
 * nhưng consumer group khác → Kafka fan-out, không ảnh hưởng nhau.
 * <p>
 * Pipeline:
 * Kafka [payment-events]
 * ↓ parse + watermark
 * ↓ Query 1: Revenue sliding window (1h / slide 5min)
 * → Redis: spark:revenue:{date}:{hour}
 * → HBase: analytics_daily (batch accumulate)
 * ↓ Query 2: Top movies (tumbling window 1h)
 * → Redis: spark:top_movies:{date} (sorted set)
 * ↓ Query 3: User behavior stats
 * → Redis: spark:user_stats:{userId}
 * <p>
 * Interviewer question: "Tại sao chạy 2 StreamingQuery riêng?"
 * → Mỗi query có output mode và trigger khác nhau.
 * Revenue cần "update" mode (chỉ emit changed rows).
 * Top movies cần "complete" mode (emit full leaderboard).
 * Không thể mix 2 output mode trong 1 query.
 * <p>
 * Submit:
 * spark-submit --class linh.vn.spark.job.AnalyticsJob \
 * --master spark://spark-master:7077 \
 * spark-processor-shaded.jar \
 * kafka:9092 localhost 2181 localhost 6379
 * ═══════════════════════════════════════════════════════════════════
 */
@Slf4j
public class AnalyticsJob {

    public static void main(String[] args) throws Exception {

        String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
        String hbaseQuorum = args.length > 1 ? args[1] : "localhost";
        String hbasePort = args.length > 2 ? args[2] : "2181";
        String redisHost = args.length > 3 ? args[3] : "localhost";
        int redisPort = args.length > 4 ? Integer.parseInt(args[4]) : 6379;

        log.info("Starting AnalyticsJob | kafka={} hbase={}:{} redis={}:{}",
                bootstrapServers, hbaseQuorum, hbasePort, redisHost, redisPort);

        HBaseSink hbaseSink = new HBaseSink(hbaseQuorum, hbasePort);
        RedisSink redisSink = new RedisSink(redisHost, redisPort);

        SparkSession spark = SparkConfig.createSession(
                "CineGoTicket-Analytics",
                "local[*]"
        );

        // ── Schema ────────────────────────────────────────────────────────────
        StructType paymentSchema = new StructType()
                .add("paymentId", DataTypes.StringType)
                .add("userId", DataTypes.StringType)
                .add("movieId", DataTypes.StringType)
                .add("amount", DataTypes.DoubleType)
                .add("status", DataTypes.StringType)
                .add("deviceIp", DataTypes.StringType)
                .add("time", DataTypes.LongType)
                .add("location", DataTypes.StringType)
                .add("returnCode", DataTypes.StringType);

        // ── Kafka source ──────────────────────────────────────────────────────
        // consumer group khác FraudDetectionJob → nhận full stream độc lập
        Dataset<Row> kafkaStream = spark
                .readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", bootstrapServers)
                .option("subscribe", "payment-events")
                // Set unique consumer group để tránh conflict với FraudDetectionJob
                .option("kafka.group.id", "analytics-group")
                // Dùng "latest" để tránh conflict với FraudDetectionJob dùng "earliest"
                .option("startingOffsets", "latest")
                .option("maxOffsetsPerTrigger", 20000)
                .load();

        // ── Parse + filter chỉ lấy PAID transactions ─────────────────────────
        // Analytics chỉ tính revenue từ giao dịch thành công
        Dataset<Row> paid = kafkaStream
                .select(
                        from_json(
                                col("value").cast(DataTypes.StringType),
                                paymentSchema
                        ).alias("data"),
                        col("timestamp").alias("kafkaTs")
                )
                .select("data.*", "kafkaTs")
                .filter(col("data").isNotNull())
                .filter(col("userId").isNotNull())
                .filter(upper(col("status")).equalTo("PAID"))
                .withColumn("eventTime",
                        when(col("time").isNotNull(),
                                col("time").divide(1000).cast(DataTypes.TimestampType))
                                .otherwise(col("kafkaTs").cast(DataTypes.TimestampType)))
                .withWatermark("eventTime", "10 minutes");

        // ═════════════════════════════════════════════════════════════════════
        // QUERY 1: Revenue theo sliding window 1h (slide 5 phút)
        // Dashboard: doanh thu từng giờ trong ngày hôm nay
        // ═════════════════════════════════════════════════════════════════════
        Dataset<Row> revenueWindowed = paid
                .groupBy(
                        window(col("eventTime"), "1 hour", "5 minutes").alias("w"),
                        date_format(col("eventTime"), "yyyy-MM-dd").alias("date"),
                        hour(col("eventTime")).alias("hour")
                )
                .agg(
                        sum("amount").alias("totalRevenue"),
                        count("*").alias("txCount"),
                        avg("amount").alias("avgAmount")
                );

        VoidFunction2<Dataset<Row>, Long> revenueProcessor = (batchDf, batchId) -> {
            List<Row> rows = batchDf.collectAsList();
            log.info("[AnalyticsJob] Revenue batch #{}: {} windows", batchId, rows.size());

            String today = LocalDate.now().toString();

            for (Row row : rows) {
                String date = row.getAs("date");
                int hour = row.getAs("hour");
                double revenue = row.getAs("totalRevenue");
                long txCount = row.getAs("txCount");

                // → Redis: real-time dashboard
                redisSink.updateRevenueByHour(date, hour, revenue);

                // → HBase: persistent analytics storage
                // Dùng movieId = "__ALL__" để represent aggregated daily revenue
                hbaseSink.writeAnalytics(date, "__ALL__", revenue, txCount);

                log.info("[AnalyticsJob] Revenue: date={} hour={} revenue={} txCount={}",
                        date, hour, revenue, txCount);
            }
        };

        StreamingQuery revenueQuery = revenueWindowed
                .writeStream()
                .outputMode("update")   // chỉ emit windows có thay đổi
//                .trigger(Trigger.ProcessingTime(String.valueOf(Duration.ofSeconds(30))))
                .trigger(Trigger.ProcessingTime("30 seconds"))
                .foreachBatch(revenueProcessor)
                .option("checkpointLocation", "/tmp/spark-checkpoint/analytics-revenue")
                .queryName("revenue-window-query")
                .start();

        // ═════════════════════════════════════════════════════════════════════
        // QUERY 2: Top movies — tumbling window 1h
        // Dashboard: top 10 phim có doanh thu cao nhất
        //
        // Tại sao TUMBLING (không overlap) thay vì SLIDING?
        // → Top movies cần leaderboard hoàn chỉnh trong 1 period
        // → COMPLETE output mode yêu cầu tumbling window
        // ═════════════════════════════════════════════════════════════════════
        Dataset<Row> topMovies = paid
                .filter(col("movieId").isNotNull())
                .groupBy(
                        window(col("eventTime"), "1 hour").alias("w"),  // tumbling
                        col("movieId")
                )
                .agg(
                        sum("amount").alias("totalRevenue"),
                        count("*").alias("txCount")
                );
//                .orderBy(desc("totalRevenue")); //bỏ orderBy vì thấy orderBy trên streaming là reject luôn, phải sort ở trong foreachBatch sau khi đã collect về driver

        VoidFunction2<Dataset<Row>, Long> topMoviesProcessor = (batchDf, batchId) -> {
            Dataset<Row> sortedDf = batchDf
                    .orderBy(desc("totalRevenue"));

            List<Row> rows = sortedDf.collectAsList();
            log.info("[AnalyticsJob] TopMovies batch #{}: {} rows", batchId, rows.size());

            String today = LocalDate.now().toString();

            for (Row row : rows) {
                String movieId = row.getAs("movieId");
                double revenue = row.getAs("totalRevenue");
                long txCount = row.getAs("txCount");

                // → Redis Sorted Set: ZINCRBY → tự động sort theo score
                redisSink.updateTopMovies(today, movieId, revenue);

                // → HBase: per-movie daily analytics
                hbaseSink.writeAnalytics(today, movieId, revenue, txCount);
            }
        };

        StreamingQuery topMoviesQuery = topMovies
                .writeStream()
                .outputMode("update")
//                .trigger(Trigger.ProcessingTime(String.valueOf(Duration.ofMinutes(1))))
                .trigger(Trigger.ProcessingTime("1 minute"))
                .foreachBatch(topMoviesProcessor)
                .option("checkpointLocation", "/tmp/spark-checkpoint/analytics-top-movies")
                .queryName("top-movies-query")
                .start();

        // ═════════════════════════════════════════════════════════════════════
        // QUERY 3: User behavior stats
        // Dashboard: avg amount per user, transaction frequency
        // ═════════════════════════════════════════════════════════════════════
        Dataset<Row> userStats = paid
                .groupBy(
                        col("userId"),
                        window(col("eventTime"), "1 hour", "15 minutes")
                )
                .agg(
                        avg("amount").alias("avgAmount"),
                        count("*").alias("txCount"),
                        sum("amount").alias("totalAmount")
                );

        VoidFunction2<Dataset<Row>, Long> userStatsProcessor = (batchDf, batchId) -> {
            List<Row> rows = batchDf.collectAsList();
            for (Row row : rows) {
                String userId = row.getAs("userId");
                double avgAmt = row.getAs("avgAmount");
                long txCount = row.getAs("txCount");

                // → Redis Hash: user stats cho dashboard
                redisSink.updateUserStats(userId, avgAmt);
            }
        };

        StreamingQuery userStatsQuery = userStats
                .writeStream()
                .outputMode("update")
//                .trigger(Trigger.ProcessingTime(String.valueOf(Duration.ofMinutes(2))))
                .trigger(Trigger.ProcessingTime("2 minutes"))
                .foreachBatch(userStatsProcessor)
                .option("checkpointLocation", "/tmp/spark-checkpoint/analytics-user-stats")
                .queryName("user-stats-query")
                .start();

        // ── Chờ tất cả queries kết thúc ──────────────────────────────────────
        log.info("[AnalyticsJob] {} streaming queries running. Awaiting termination...",
                spark.streams().active().length);

        // Chạy vô hạn thay vì awaitAnyTermination
        while (true) {
            try {
                Thread.sleep(60000); // Sleep 1 phút
                log.info("[AnalyticsJob] {} queries still running...", spark.streams().active().length);
            } catch (InterruptedException e) {
                log.info("[AnalyticsJob] Interrupted, shutting down...");
                break;
            }
        }
    }
}