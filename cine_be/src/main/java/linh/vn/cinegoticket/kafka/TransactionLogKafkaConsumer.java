package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.log.TransactionLog;
import linh.vn.cinegoticket.log.TransactionLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TransactionLogKafkaConsumer {

    private final TransactionLogRepository logRepository;

    public TransactionLogKafkaConsumer(TransactionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @KafkaListener(topics = "transaction-logs-topic", groupId = "cinego-log-group")
    public void consume(TransactionLog log) {
        System.out.println("Received log at: " + LocalDateTime.now() + " - Action: " + log.getAction());
        logRepository.save(log);
    }
}

