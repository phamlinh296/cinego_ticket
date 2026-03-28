package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;

import linh.vn.cinegoticket.service.impl.AnomalyDetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyConsumer {

    private final AnomalyDetectorService anomalyService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2000)
    )
    @KafkaListener(
            topics = "payment-events",
            groupId = "anomaly-detector-group"
            ,containerFactory = "kafkaListenerContainerFactory" //đã khai báo trong application.yml
            // nếu k kb trong application.yml thì phải khai báo như này và có KafkaConfig
    )
    public void onPaymentEvent(PaymentEvent event) {

        log.info("Received payment event: {} user: {} amount: {}",
                event.getPaymentId(), event.getUserId(), event.getAmount());

        anomalyService.analyze(event);

        log.info("Finished analyzing payment event {}", event.getPaymentId());
    }
}
