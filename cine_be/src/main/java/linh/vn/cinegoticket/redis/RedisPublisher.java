package linh.vn.cinegoticket.redis;

import linh.vn.cinegoticket.dto.response.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
@Slf4j
@Component
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;//thao tác với Redis.
    private final ChannelTopic topic;

    public RedisPublisher(RedisTemplate<String, Object> redisTemplate, ChannelTopic topic) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    public void sendMessage(PaymentResponse message) {
        try {
            redisTemplate.convertAndSend(topic.getTopic(), message);
            log.info("RedisPublisher ✅ Message sent successfully to Redis: " + message);
        } catch (Exception e) {
            log.info("RedisPublisher ❌ Error publishing message to Redis: " + e.getMessage());
        }
    }

}
