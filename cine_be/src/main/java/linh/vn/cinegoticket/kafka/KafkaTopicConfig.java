package linh.vn.cinegoticket.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

//tuy auto.create.topics.enable=true config này trong Kafka server (container) có the tu dong tạo topic này mà k cần cấu hình class này
//nhưng trong product luôn nên tạo topic thủ công ntnay để tránh lỗi và custom đc
@Configuration
public class KafkaTopicConfig {
    @Bean
    public NewTopic transactionLogsTopic() {
        return TopicBuilder.name("transaction-logs-topic")
                .partitions(3)
                .replicas(1)
                .build();
    }
    //cinego-log-group: partitions assigned: [transaction-logs-topic-0, transaction-logs-topic-1, transaction-logs-topic-2]
}

//nếu thiếu topic >> Khi gửi message từ Producer: ✅ Gây lỗi: UnknownTopicOrPartitionException nếu topic không tồn tại:
//org.apache.kafka.common.errors.UnknownTopicOrPartitionException: The topic 'transaction-logs-topic' does not exist and auto-create is disabled.