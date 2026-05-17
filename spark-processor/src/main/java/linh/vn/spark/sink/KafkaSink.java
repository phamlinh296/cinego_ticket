package linh.vn.spark.sink;

import com.fasterxml.jackson.databind.ObjectMapper;
import linh.vn.spark.model.FraudAlert;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

/**
 * KafkaSink: Spark publish FraudAlert → Kafka topic "anomaly-events".
 * <p>
 * ═══════════════════════════════════════════════════
 * Tại sao Spark cần ghi vào Kafka thay vì chỉ HBase?
 * ═══════════════════════════════════════════════════
 * - HBase: storage, query sau
 * - Kafka: event-driven downstream (notification service, block-user service, v.v.)
 * - Downstream consumer (Spring) sẽ đọc "anomaly-events" để gửi email/block user
 * <p>
 * Đây là pattern "Kafka as event bus" — Spark là producer,
 * nhiều consumer có thể subscribe mà không cần biết nhau.
 * <p>
 * ═══════════════════════════════════════════════════
 * KafkaProducer trong Spark Executor:
 * ═══════════════════════════════════════════════════
 * Tương tự RedisSink: dùng static instance để tái sử dụng producer
 * across batches trong cùng executor. Producer là thread-safe.
 */
@Slf4j
public class KafkaSink implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String TOPIC_ANOMALY_EVENTS = "anomaly-events";

    private static volatile KafkaProducer<String, String> producer;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String bootstrapServers;

    public KafkaSink(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    private KafkaProducer<String, String> getProducer() {
        if (producer == null) {
            synchronized (KafkaSink.class) {
                if (producer == null) {
                    Properties props = new Properties();
                    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                    // Đảm bảo message không bị mất khi broker restart
                    props.put(ProducerConfig.ACKS_CONFIG, "all");
                    props.put(ProducerConfig.RETRIES_CONFIG, 3);
                    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
                    props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
                    props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "15000");
                    props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "10000");
                    producer = new KafkaProducer<>(props);
                    log.info("[KafkaSink] KafkaProducer created: {}", bootstrapServers);
                }
            }
        }
        return producer;
    }

    /**
     * Publish danh sách FraudAlert ra Kafka.
     * Key = userId để đảm bảo các alert của cùng 1 user vào cùng partition (ordering).
     */
    public void publishFraudAlerts(List<FraudAlert> alerts) {
        if (alerts == null || alerts.isEmpty()) return;
        KafkaProducer<String, String> prod = getProducer();

        for (FraudAlert alert : alerts) {
            try {
                String json = objectMapper.writeValueAsString(alert);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        TOPIC_ANOMALY_EVENTS,
                        alert.getUserId(),  // key → same partition cho cùng user
                        json
                );
                // Async send với callback để log error
                prod.send(record, (metadata, ex) -> {
                    if (ex != null) {
                        log.error("[KafkaSink] Failed to send alert {}: {}", alert.getPaymentId(), ex.getMessage());
                    } else {
                        log.debug("[KafkaSink] Alert sent: topic={} partition={} offset={}",
                                metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
            } catch (Exception e) {
                log.error("[KafkaSink] Serialization failed for alert {}: {}", alert.getPaymentId(), e.getMessage());
            }
        }
        log.info("[KafkaSink] Published {} fraud alerts to topic {} (async, no flush)", alerts.size(), TOPIC_ANOMALY_EVENTS);
    }
}