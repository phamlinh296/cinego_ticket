package linh.vn.cinegoticket.config.kafka;

import linh.vn.cinegoticket.dto.PaymentEvent;

import linh.vn.cinegoticket.service.impl.AnomalyDetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyConsumer {

    private final AnomalyDetectorService anomalyService;

    @KafkaListener(
            topics = "payment-events",
            groupId = "anomaly-detector-group"
            ,containerFactory = "kafkaListenerContainerFactory" //đã khai báo trong application.yml
            // nếu k kb trong application.yml thì phải khai báo như này và có KafkaConfig
    )
    public void onPaymentEvent(PaymentEvent event) {

        log.info("Received payment event: {} user: {} amount: {}",
                event.getPaymentId(), event.getUserId(), event.getAmount());

        try {
            anomalyService.analyze(event);
        } catch (Exception ex) {
            log.error("Error analyzing payment event {} : {}", event.getPaymentId(), ex.getMessage(), ex);
        }
    }
}
