package linh.vn.cinegoticket.kafka;


import linh.vn.cinegoticket.log.TransactionLog;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.CompletableFuture;

@Service
public class TransactionLogKafkaPublisher {
    private final KafkaTemplate<String, TransactionLog> kafkaTemplate;
    //nếu ghi ntn thì n sd KafkaTemplate mặc đinh dc config trong application.yml

    public TransactionLogKafkaPublisher(KafkaTemplate<String, TransactionLog> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

//    public void sendLog(TransactionLog log) {
//        ListenableFuture<SendResult<String, TransactionLog>> future =
//                (ListenableFuture<SendResult<String, TransactionLog>>) kafkaTemplate.send("transaction-logs-topic", log);
//
//        future.addCallback(
//                success -> System.out.println("✅ Send SUCCESS: " + log.getAction()),
//                failure -> System.err.println("❌ Send FAILED: " + failure.getMessage())
//        );
//    }
    public void sendLog(TransactionLog log) {
        CompletableFuture<SendResult<String, TransactionLog>> future =
                kafkaTemplate.send("transaction-logs-topic", log);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("✅ Send SUCCESS: " + log.getAction());
            } else {
                System.err.println("❌ Send FAILED: " + ex.getMessage());
            }
        });
    }


}

