package linh.vn.spark.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Fraud alert được Spark generate và publish ra Kafka + lưu HBase.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAlert implements Serializable {

    private static final long serialVersionUID = 1L;

    private String paymentId;
    private String userId;
    private String alertType;    // SMURFING, ZSCORE_DEEP, HIGH_FREQUENCY_WINDOW
    private double riskScore;
    private double amount;
    private String description;
    private long detectedAt;  // epoch milli
}