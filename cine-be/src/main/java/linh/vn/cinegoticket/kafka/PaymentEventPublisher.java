package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(PaymentEvent event) {

        kafkaTemplate.send("payment-events", event.getMovieId(), event);

        log.info("Published event {}", event.getPaymentId());
    }
}