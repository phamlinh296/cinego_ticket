package linh.vn.cinegoticket.kafka;

import linh.vn.cinegoticket.log.TransactionLog;
import linh.vn.cinegoticket.log.TransactionLogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransactionLogConsumer {

    private final TransactionLogRepository logRepository;

    public TransactionLogConsumer(TransactionLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @KafkaListener(topics = "transaction-logs-topic", groupId = "cinego-log-group")
    public void consume(TransactionLog log) {
        logRepository.save(log); // Ghi vào MongoDB
        System.out.println("Log saved: " + log.getAction());
    }
}

