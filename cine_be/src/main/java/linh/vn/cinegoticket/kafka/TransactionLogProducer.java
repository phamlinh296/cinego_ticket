package linh.vn.cinegoticket.kafka;


import linh.vn.cinegoticket.log.TransactionLog;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionLogProducer {
    private final KafkaTemplate<String, TransactionLog> kafkaTemplate;

    public TransactionLogProducer(KafkaTemplate<String, TransactionLog> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendLog(TransactionLog log) {
        kafkaTemplate.send("transaction-logs-topic", log);
    }
}

