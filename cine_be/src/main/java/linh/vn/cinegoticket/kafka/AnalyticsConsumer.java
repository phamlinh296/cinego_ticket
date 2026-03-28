package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import linh.vn.cinegoticket.processor.AnalyticsProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsConsumer {

    private final AnalyticsProcessor analyticsProcessor;
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000),
            dltTopicSuffix = "-dlq"
    )
    @KafkaListener(
            topics = "payment-events",
            groupId = "analytics-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(PaymentEvent event) {
        analyticsProcessor.process(event);
    }

    // xử lý DLQ
    @KafkaListener(
            topics = "payment-events-dlq",
            groupId = "analytics-dlq-group"
    )
    public void handleDLQ(PaymentEvent event) {
        log.error("DLQ EVENT: {}", event.getPaymentId());
        // TODO:
        // - save DB
        // - alert
        // - retry manual
    }

}