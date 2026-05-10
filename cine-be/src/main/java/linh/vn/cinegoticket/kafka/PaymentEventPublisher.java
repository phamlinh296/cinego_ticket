package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(PaymentEvent event) {

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send("payment-events", event.getMovieId(), event);
        
        future.whenComplete((result, error) -> {
            if (error == null) {
                log.info("✅ Successfully published payment event {} to partition {} offset {}", 
                        event.getPaymentId(), 
                        result.getRecordMetadata().partition(), 
                        result.getRecordMetadata().offset());
            } else {
                log.error("❌ Failed to publish payment event {}: {}", 
                        event.getPaymentId(), 
                        error.getMessage(), 
                        error);
            }
        });

        log.info("Publishing event {}", event.getPaymentId());
    }
}