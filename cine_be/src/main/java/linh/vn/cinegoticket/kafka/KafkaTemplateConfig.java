//package linh.vn.cinegoticket.kafka;
//
//import linh.vn.cinegoticket.log.TransactionLog;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.core.ProducerFactory;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Configuration
//public class KafkaTemplateConfig {
//
//    @Bean
//    public KafkaTemplate<String, TransactionLog> kafkaTemplate0(ProducerFactory<String, TransactionLog> pf) {
//        Map<String, Object> props = new HashMap<>(pf.getConfigurationProperties());
//        props.put(ProducerConfig.ACKS_CONFIG, "0");
//        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
//    }
//
//    @Bean
//    public KafkaTemplate<String, TransactionLog> kafkaTemplate1(ProducerFactory<String, TransactionLog> pf) {
//        Map<String, Object> props = new HashMap<>(pf.getConfigurationProperties());
//        props.put(ProducerConfig.ACKS_CONFIG, "1");
//        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
//    }
//
//    @Bean
//    public KafkaTemplate<String, TransactionLog> kafkaTemplateAll(ProducerFactory<String, TransactionLog> pf) {
//        Map<String, Object> props = new HashMap<>(pf.getConfigurationProperties());
//        props.put(ProducerConfig.ACKS_CONFIG, "all");
//        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
//    }
//}
//
