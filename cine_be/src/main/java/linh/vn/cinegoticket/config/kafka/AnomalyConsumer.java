package linh.vn.cinegoticket.config.kafka;

import linh.vn.cinegoticket.dto.PaymentEvent;
import linh.vn.cinegoticket.service.impl.AnomalyDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AnomalyConsumer {

    @Autowired
    private AnomalyDetectionService anomalyService;

    @KafkaListener(topics = "payment-events", groupId = "anomaly-group")
    public void consumePaymentEvent(PaymentEvent event) {
        anomalyService.detect(event);
    }
}
