package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import linh.vn.cinegoticket.service.impl.AnomalyDetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyConsumer {

    private final AnomalyDetectorService anomalyService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "anomaly-detector-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentEvent(PaymentEvent event) {

        log.info("Received payment event: {} user: {} amount: {}",
                event.getPaymentId(), event.getUserId(), event.getAmount());

        anomalyService.analyze(event);

        log.info("Finished analyzing payment event {}", event.getPaymentId());
    }
}
