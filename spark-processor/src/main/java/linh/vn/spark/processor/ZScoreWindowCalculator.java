package linh.vn.spark.processor;

import linh.vn.spark.model.FraudAlert;
import linh.vn.spark.model.PaymentEvent;
import linh.vn.spark.sink.HBaseSink;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

/**
 * ZScoreWindowCalculator: tính Z-Score dùng lịch sử 30 ngày từ HBase.
 * <p>
 * ═══════════════════════════════════════════════════
 * Khác với ZScoreService trong Spring (cine-be):
 * ═══════════════════════════════════════════════════
 * Spring ZScoreService:
 * - History = Redis cache (20 records gần nhất)
 * - Fast, per-event, <5ms
 * - Window ngắn → false negative cho user có behavior biến động
 * <p>
 * Spark ZScoreWindowCalculator (class này):
 * - History = HBase (tối đa 500 records, ~30 ngày)
 * - Chậm hơn nhưng chạy trong batch (micro-batch 30s)
 * - Window dài → chính xác hơn, ít false positive hơn
 * <p>
 * Kết hợp cả hai: Spring detect nhanh (high recall),
 * Spark confirm với deep analysis (high precision).
 */
@Slf4j
public class ZScoreWindowCalculator implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double Z_THRESHOLD = 3.0;  // same as Spring
    private static final int MIN_HISTORY = 10;   // cần ít nhất 10 records
    private static final int MAX_HISTORY = 500;  // lấy tối đa 500 records từ HBase

    private final HBaseSink hbaseSink;

    public ZScoreWindowCalculator(HBaseSink hbaseSink) {
        this.hbaseSink = hbaseSink;
    }

    /**
     * Kiểm tra Z-Score cho một payment event, dùng HBase history.
     *
     * @param event payment event cần check
     * @return FraudAlert nếu là outlier, null nếu bình thường
     */
    public FraudAlert check(PaymentEvent event) {
        if (event.getUserId() == null) return null;

        // Lấy lịch sử từ HBase
        List<Double> history = hbaseSink.readPaymentHistory(event.getUserId(), MAX_HISTORY);

        if (history.size() < MIN_HISTORY) {
            log.debug("[ZScore] Not enough history for user={}: {} records (need {})",
                    event.getUserId(), history.size(), MIN_HISTORY);
            return null;
        }

        double mean = history.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = history.stream()
                .mapToDouble(a -> Math.pow(a - mean, 2))
                .sum() / history.size();
        double std = Math.sqrt(variance);

        if (std == 0.0) return null;

        double z = (event.getAmount() - mean) / std;

        if (Math.abs(z) > Z_THRESHOLD) {
            log.warn("[ZScore] OUTLIER detected: user={} amount={} mean={:.0f} std={:.0f} z={:.2f}",
                    event.getUserId(), event.getAmount(), mean, std, z);

            return FraudAlert.builder()
                    .paymentId(event.getPaymentId())
                    .userId(event.getUserId())
                    .alertType("ZSCORE_DEEP")
                    .riskScore(Math.min(0.99, 0.8 + (Math.abs(z) - Z_THRESHOLD) * 0.05))
                    .amount(event.getAmount())
                    .description(String.format(
                            "Deep Z-Score outlier: z=%.2f (mean=%.0f, std=%.0f, history=%d records)",
                            z, mean, std, history.size()))
                    .detectedAt(System.currentTimeMillis())
                    .build();
        }

        return null;
    }
}