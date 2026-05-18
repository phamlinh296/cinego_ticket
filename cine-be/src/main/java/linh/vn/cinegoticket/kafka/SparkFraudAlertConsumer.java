package linh.vn.cinegoticket.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.enums.AnomalyType;
import linh.vn.cinegoticket.enums.DetectionSource;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SparkFraudAlertConsumer {

    private final AnomalyLogRepository anomalyLogRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "anomaly-events",
            groupId = "spark-fraud-alert-consumer",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onSparkFraudAlert(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            // Spark FraudAlert có field "alertType"; Spring AnomalyLog có field "type" — skip nếu không phải Spark
            if (!node.has("alertType")) {
                return;
            }

            String alertType = node.path("alertType").asText();
            AnomalyType anomalyType = mapAlertType(alertType);

            AnomalyLog anomalyLog = new AnomalyLog();
            anomalyLog.setPaymentId(node.path("paymentId").asText(null));
            anomalyLog.setUserId(node.path("userId").asText(null));
            anomalyLog.setType(anomalyType);
            anomalyLog.setSource(DetectionSource.SPARK);
            anomalyLog.setRiskScore(node.path("riskScore").asDouble(0.0));
            anomalyLog.setAmount(node.path("amount").asDouble(0.0));
            anomalyLog.setDescription("[Spark] " + node.path("description").asText(""));

            anomalyLogRepository.save(anomalyLog);
            log.info("Saved Spark fraud alert: type={} user={} payment={}",
                    anomalyType, anomalyLog.getUserId(), anomalyLog.getPaymentId());

        } catch (Exception e) {
            log.error("Failed to process Spark anomaly event: {}", e.getMessage());
        }
    }

    private AnomalyType mapAlertType(String alertType) {
        return switch (alertType) {
            case "SMURFING" -> AnomalyType.SMURFING;
            case "ZSCORE_DEEP" -> AnomalyType.ZSCORE_DEEP;
            case "HIGH_FREQUENCY_WINDOW" -> AnomalyType.HIGH_FREQUENCY_WINDOW;
            default -> {
                log.warn("Unknown Spark alertType '{}', defaulting to HIGH_AMOUNT", alertType);
                yield AnomalyType.HIGH_AMOUNT;
            }
        };
    }
}
