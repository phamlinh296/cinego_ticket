package linh.vn.shared.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * FraudAlert — Kafka message contract cho topic "anomaly-events".
 * <p>
 * ════════════════════════════════════════════════════════
 * Producer: Spark FraudDetectionJob (window-based patterns)
 * Spring AnomalyDetectorService (rule-based, per-event)
 * <p>
 * Consumer: (future) NotificationService — gửi email/SMS cảnh báo
 * (future) UserBlockService — block user có risk score cao
 * (future) DashboardService — real-time fraud dashboard
 * ════════════════════════════════════════════════════════
 * <p>
 * alertType values:
 * MULTIPLE_FAST_TRANSACTION — Spring: ≥3 tx trong 30s
 * FAILED_BUT_CHARGED        — Spring: gateway conflict
 * DEVICE_CHANGE             — Spring: IP thay đổi
 * HIGH_AMOUNT               — Spring: vượt threshold cố định
 * AMOUNT_ZSCORE             — Spring: Z-Score outlier (Redis 20 records)
 * SMURFING                  — Spark: nhiều tx nhỏ → 1 tx lớn (1h window)
 * HIGH_FREQUENCY_WINDOW     — Spark: ≥10 tx trong 1h window
 * ZSCORE_DEEP               — Spark: Z-Score outlier (HBase 500 records)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FraudAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("userId")
    private String userId;

    /**
     * Xem danh sách alertType ở Javadoc class.
     */
    @JsonProperty("alertType")
    private String alertType; //// SMURFING, ZSCORE_DEEP, HIGH_FREQUENCY_WINDOW

    /**
     * Risk score từ 0.0 → 1.0.
     * > 0.9: critical, block ngay
     * 0.7–0.9: high, cần review
     * < 0.7: medium, log và monitor
     */
    @JsonProperty("riskScore")
    private double riskScore;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("description")
    private String description;

    /**
     * Epoch milliseconds khi alert được tạo.
     */
    @JsonProperty("detectedAt")
    private long detectedAt;

    /**
     * Source của alert để phân biệt khi consume.
     * "SPRING" | "SPARK"
     */
    @JsonProperty("source")
    private String source;
}