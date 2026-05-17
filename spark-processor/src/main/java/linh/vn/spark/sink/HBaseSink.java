package linh.vn.spark.sink;

import linh.vn.spark.model.FraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * HBaseSink: ghi data từ Spark foreachBatch vào HBase.
 * <p>
 * ═══════════════════════════════════════════════════
 * QUAN TRỌNG về Serialization trong Spark:
 * ═══════════════════════════════════════════════════
 * Spark serialize lambda/closure để gửi đến executor.
 * HBase Connection KHÔNG serializable → phải tạo Connection
 * trong executor (không phải driver).
 * <p>
 * Pattern: tạo Connection lazily trong foreachBatch,
 * dùng try-with-resources để đóng sau mỗi batch.
 * <p>
 * Trong production: dùng connection pool hoặc broadcast variable.
 */
@Slf4j
public class HBaseSink implements Serializable {

    private static final long serialVersionUID = 1L;

    // HBase table names
    public static final String TABLE_FRAUD_LOGS = "fraud_logs";
    public static final String TABLE_PAYMENT_HISTORY = "payment_history";
    public static final String TABLE_ANALYTICS_DAILY = "analytics_daily";

    // Column family (tất cả tables dùng chung "cf" cho đơn giản)
    private static final byte[] CF = Bytes.toBytes("cf");

    private final String zookeeperQuorum;
    private final String zookeeperPort;

    public HBaseSink(String zookeeperQuorum, String zookeeperPort) {
        this.zookeeperQuorum = zookeeperQuorum;
        this.zookeeperPort = zookeeperPort;
    }

    /**
     * Tạo HBase Connection mới.
     * Gọi trong executor context (bên trong foreachBatch/foreachPartition).
     */
    private Connection createConnection() throws IOException {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", zookeeperQuorum);
        config.set("hbase.zookeeper.property.clientPort", zookeeperPort);
        config.set("hbase.rpc.timeout", "5000");
        config.set("hbase.client.operation.timeout", "10000");
        config.set("hbase.client.retries.number", "3");
        config.set("zookeeper.session.timeout", "10000");
        config.set("zookeeper.recovery.retry", "1");
        return ConnectionFactory.createConnection(config);
    }

    /**
     * Ghi danh sách FraudAlert vào table fraud_logs.
     * <p>
     * Row key: {userId}_{alertType}_{reverseTimestamp}
     * → Scan theo user nhanh, sort newest first
     */
    public void writeFraudAlerts(List<FraudAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) return;

        try (Connection conn = createConnection();
             Table table = conn.getTable(TableName.valueOf(TABLE_FRAUD_LOGS))) {

            List<Put> puts = new java.util.ArrayList<>();
            for (FraudAlert alert : alerts) {
                long reverseTs = Long.MAX_VALUE - alert.getDetectedAt();
                String rowKeyStr = alert.getUserId() + "_"
                        + alert.getAlertType() + "_"
                        + String.format("%019d", reverseTs);
                byte[] rowKey = Bytes.toBytes(rowKeyStr);

                Put put = new Put(rowKey);
                put.addColumn(CF, Bytes.toBytes("paymentId"), Bytes.toBytes(alert.getPaymentId() != null ? alert.getPaymentId() : ""));
                put.addColumn(CF, Bytes.toBytes("userId"), Bytes.toBytes(alert.getUserId()));
                put.addColumn(CF, Bytes.toBytes("alertType"), Bytes.toBytes(alert.getAlertType()));
                put.addColumn(CF, Bytes.toBytes("riskScore"), Bytes.toBytes(alert.getRiskScore()));
                put.addColumn(CF, Bytes.toBytes("amount"), Bytes.toBytes(alert.getAmount()));
                put.addColumn(CF, Bytes.toBytes("description"), Bytes.toBytes(alert.getDescription() != null ? alert.getDescription() : ""));
                put.addColumn(CF, Bytes.toBytes("detectedAt"), Bytes.toBytes(alert.getDetectedAt()));
                puts.add(put);
            }
            table.put(puts); // batch put — 1 RPC thay vì N RPC
            log.info("[HBaseSink] Wrote {} fraud alerts to HBase", alerts.size());

        } catch (IOException e) {
            // Log và tiếp tục — fraud alert đã publish lên Kafka + Redis trước đó.
            // HBase là audit storage, không phải primary sink, nên không kill query.
            log.error("[HBaseSink] Failed to write fraud alerts (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Ghi analytics aggregation vào analytics_daily.
     * <p>
     * Row key: {date}_{movieId}
     * → Scan theo ngày nhanh
     * → Increment counter để tránh race condition
     *
     * @param date    "2026-04-19"
     * @param movieId movie ID
     * @param revenue tổng revenue của batch này
     * @param txCount số transaction của batch này
     */
    public void writeAnalytics(String date, String movieId, double revenue, long txCount) {
        String rowKeyStr = date + "_" + movieId;
        byte[] rowKey = Bytes.toBytes(rowKeyStr);

        try (Connection conn = createConnection();
             Table table = conn.getTable(TableName.valueOf(TABLE_ANALYTICS_DAILY))) {

            // Dùng Increment để atomic add — safe khi nhiều executor cùng write
            Increment increment = new Increment(rowKey);
            increment.addColumn(CF, Bytes.toBytes("tx_count"), txCount);
            // Revenue: lưu dưới dạng long (VND không có decimal)
            increment.addColumn(CF, Bytes.toBytes("revenue"), (long) revenue);
            table.increment(increment);

            log.debug("[HBaseSink] Analytics updated: date={} movieId={} revenue={} count={}",
                    date, movieId, revenue, txCount);

        } catch (IOException e) {
            // Log và tiếp tục — HBase write failure không được kill streaming query.
            // Data sẽ được ghi lại ở batch tiếp theo sau khi HBase hồi phục.
            log.error("[HBaseSink] Failed to write analytics (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Ghi PaymentEvent vào payment_history để ZScoreWindowCalculator đọc lại.
     * Row key giống readPaymentHistory → idempotent khi sliding window ghi lặp.
     */
    public void writePaymentHistory(List<linh.vn.spark.model.PaymentEvent> events) {
        if (events == null || events.isEmpty()) return;

        try (Connection conn = createConnection();
             Table table = conn.getTable(TableName.valueOf(TABLE_PAYMENT_HISTORY))) {

            List<Put> puts = new java.util.ArrayList<>();
            for (linh.vn.spark.model.PaymentEvent event : events) {
                long time = event.getTime() != null ? event.getTime() : System.currentTimeMillis();
                long reverseTs = Long.MAX_VALUE - time;
                String rowKeyStr = event.getUserId() + "_" + String.format("%019d", reverseTs);
                Put put = new Put(Bytes.toBytes(rowKeyStr));
                put.addColumn(CF, Bytes.toBytes("amount"), Bytes.toBytes(event.getAmount()));
                puts.add(put);
            }
            table.put(puts);
            log.info("[HBaseSink] Wrote {} records to payment_history", puts.size());

        } catch (IOException e) {
            log.error("[HBaseSink] Failed to write payment_history (non-fatal): {}", e.getMessage());
        }
    }

    /**
     * Đọc lịch sử payment của user để tính Z-Score trong Spark.
     *
     * @param userId user ID
     * @param limit  số bản ghi cần lấy
     * @return danh sách amount (newest first)
     */
    public java.util.List<Double> readPaymentHistory(String userId, int limit) {
        java.util.List<Double> amounts = new java.util.ArrayList<>();
        byte[] prefix = Bytes.toBytes(userId + "_");

        Scan scan = new Scan();
        scan.setRowPrefixFilter(prefix);
        scan.addColumn(CF, Bytes.toBytes("amount"));
        scan.setLimit(limit);
        scan.readVersions(1);

        try (Connection conn = createConnection();
             Table table = conn.getTable(TableName.valueOf(TABLE_PAYMENT_HISTORY));
             ResultScanner scanner = table.getScanner(scan)) {

            for (Result result : scanner) {
                byte[] val = result.getValue(CF, Bytes.toBytes("amount"));
                if (val != null) amounts.add(Bytes.toDouble(val));
            }
        } catch (IOException e) {
            log.error("[HBaseSink] Failed to read history for user={}: {}", userId, e.getMessage(), e);
        }
        return amounts;
    }
}