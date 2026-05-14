package linh.vn.spark.processor;

import linh.vn.spark.model.FraudAlert;
import linh.vn.spark.model.PaymentEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PatternDetector: detect các fraud pattern phức tạp cần window aggregation.
 * <p>
 * ═══════════════════════════════════════════════════
 * SMURFING PATTERN (pattern quan trọng nhất):
 * ═══════════════════════════════════════════════════
 * Định nghĩa: User thực hiện nhiều giao dịch nhỏ (để tránh threshold),
 * sau đó 1 giao dịch lớn bất thường.
 * <p>
 * Ví dụ thực tế: Credit card fraud, money laundering
 * - Tx 1: 50,000 VND
 * - Tx 2: 75,000 VND
 * - Tx 3: 60,000 VND
 * - Tx 4: 5,000,000 VND ← anomaly
 * <p>
 * Spark phát hiện được pattern này vì nó có FULL WINDOW VIEW
 * (tất cả transactions trong 1 giờ của user).
 * Spring AnomalyDetector chỉ thấy từng event đơn lẻ.
 * <p>
 * ═══════════════════════════════════════════════════
 * HIGH_FREQUENCY_WINDOW:
 * ═══════════════════════════════════════════════════
 * ≥ 10 transactions trong 1 giờ → suspicious behavior.
 * Khác với Spring's fast-tx (≥3 trong 30s) — Spark detect frequency thấp hơn
 * nhưng trên window dài hơn.
 */
@Slf4j
public class PatternDetector implements Serializable {

    private static final long serialVersionUID = 1L;

    // Smurfing: nhiều tx nhỏ → 1 tx lớn
    private static final int SMURFING_MIN_SMALL_TX = 3;       // số tx nhỏ tối thiểu
    private static final double SMURFING_SMALL_THRESHOLD = 200_000; // tx <= 200k coi là "nhỏ"
    private static final double SMURFING_LARGE_MULTIPLIER = 10.0;  // tx lớn gấp 10x avg của tx nhỏ
    private static final double SMURFING_LARGE_MIN = 1_000_000; // tx lớn tối thiểu 1M VND

    // High frequency
    private static final int HIGH_FREQ_COUNT = 10;  // ≥10 tx trong window

    /**
     * Phân tích danh sách events của 1 user trong 1 window.
     * Gọi trong foreachBatch sau khi groupBy(userId).
     *
     * @param userId user ID
     * @param events tất cả events của user trong window
     * @return danh sách FraudAlert được detect (có thể rỗng)
     */
    public List<FraudAlert> detect(String userId, List<PaymentEvent> events) {
        List<FraudAlert> alerts = new ArrayList<>();

        if (events == null || events.isEmpty()) return alerts;

        // ── Pattern 1: Smurfing ────────────────────────────────────────────────
        FraudAlert smurfing = detectSmurfing(userId, events);
        if (smurfing != null) alerts.add(smurfing);

        // ── Pattern 2: High frequency within window ────────────────────────────
        if (events.size() >= HIGH_FREQ_COUNT) {
            alerts.add(FraudAlert.builder()
                    .paymentId("multi_" + userId + "_" + System.currentTimeMillis())
                    .userId(userId)
                    .alertType("HIGH_FREQUENCY_WINDOW")
                    .riskScore(0.75)
                    .amount(events.stream().mapToDouble(PaymentEvent::getAmount).sum())
                    .description(String.format("%d transactions in 1h window (threshold: %d)",
                            events.size(), HIGH_FREQ_COUNT))
                    .detectedAt(System.currentTimeMillis())
                    .build());
            log.warn("[PatternDetector] HIGH_FREQUENCY user={} count={}", userId, events.size());
        }

        return alerts;
    }

    private FraudAlert detectSmurfing(String userId, List<PaymentEvent> events) {
        // Tách thành tx nhỏ và tx lớn
        List<PaymentEvent> smallTxs = new ArrayList<>();
        List<PaymentEvent> largeTxs = new ArrayList<>();

        for (PaymentEvent e : events) {
            if (e.getAmount() <= SMURFING_SMALL_THRESHOLD) {
                smallTxs.add(e);
            } else if (e.getAmount() >= SMURFING_LARGE_MIN) {
                largeTxs.add(e);
            }
        }

        // Cần ≥3 tx nhỏ VÀ ≥1 tx lớn
        if (smallTxs.size() < SMURFING_MIN_SMALL_TX || largeTxs.isEmpty()) return null;

        double avgSmall = smallTxs.stream()
                .mapToDouble(PaymentEvent::getAmount)
                .average()
                .orElse(0.0);

        // Kiểm tra: tx lớn nhất có gấp X lần avg tx nhỏ không
        double maxLarge = largeTxs.stream()
                .mapToDouble(PaymentEvent::getAmount)
                .max()
                .orElse(0.0);

        if (maxLarge >= avgSmall * SMURFING_LARGE_MULTIPLIER) {
            double riskScore = Math.min(0.99, 0.7 + (smallTxs.size() * 0.05));

            log.warn("[PatternDetector] SMURFING detected: user={} smallTxCount={} avgSmall={} maxLarge={}",
                    userId, smallTxs.size(), avgSmall, maxLarge);

            return FraudAlert.builder()
                    .paymentId(largeTxs.get(0).getPaymentId() != null
                            ? largeTxs.get(0).getPaymentId()
                            : "smurf_" + userId + "_" + System.currentTimeMillis())
                    .userId(userId)
                    .alertType("SMURFING")
                    .riskScore(riskScore)
                    .amount(maxLarge)
                    .description(String.format(
                            "Smurfing: %d small txs (avg=%.0f) followed by large tx (%.0f, ratio=%.1fx)",
                            smallTxs.size(), avgSmall, maxLarge, maxLarge / avgSmall))
                    .detectedAt(System.currentTimeMillis())
                    .build();
        }

        return null;
    }
}