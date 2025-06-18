//package linh.vn.cinegoticket.kafka;
//
//import linh.vn.cinegoticket.log.TransactionLog;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//public class KafkaService {
//
//    private final KafkaTemplate<String, TransactionLog> kafkaTemplate0;
//    private final KafkaTemplate<String, TransactionLog> kafkaTemplate1;
//    private final KafkaTemplate<String, TransactionLog> kafkaTemplateAll;
//
//    public KafkaService(KafkaTemplate<String, TransactionLog> kafkaTemplate0,
//                        KafkaTemplate<String, TransactionLog> kafkaTemplate1,
//                        KafkaTemplate<String, TransactionLog> kafkaTemplateAll) {
//        this.kafkaTemplate0 = kafkaTemplate0;
//        this.kafkaTemplate1 = kafkaTemplate1;
//        this.kafkaTemplateAll = kafkaTemplateAll;
//    }
//
//    public void sendLogWithAcks(TransactionLog log, String acksMode) {
//        switch (acksMode) {
//            case "0" -> kafkaTemplate0.send("transaction-logs-topic", log);
//            case "1" -> kafkaTemplate1.send("transaction-logs-topic", log);
//            case "all" -> kafkaTemplateAll.send("transaction-logs-topic", log);
//            default -> throw new IllegalArgumentException("Invalid acksMode");
//        }
//    }
//}
//
