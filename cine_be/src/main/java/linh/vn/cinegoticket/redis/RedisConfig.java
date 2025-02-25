package linh.vn.cinegoticket.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@Slf4j
public class RedisConfig {
    @Bean
    @Primary
    public CacheManager listCacheManager(final RedisConnectionFactory redisConnectionFactory) {
        return build(redisConnectionFactory, new Jackson2JsonRedisSerializer<>(Object.class), Duration.ofMinutes(10)); // TTL 10 phút
    }

    @Bean
    public CacheManager objectCacheManager(final RedisConnectionFactory redisConnectionFactory) {
        return build(redisConnectionFactory, RedisSerializer.json(), Duration.ofHours(1)); // TTL 1 giờ
    }

    private CacheManager build(final RedisConnectionFactory redisConnectionFactory,
                               final RedisSerializer<?> redisSerializer,
                               final Duration ttl) {
        final var serializer = RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer);
        return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(redisConnectionFactory)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                        .disableCachingNullValues()
                        .entryTtl(ttl) // Set TTL cho cache
                        .serializeValuesWith(serializer))
                .build();
    }

    //PUB- SUB
    //1. Tạo RedisTemplate để gửi/nhận dữ liệu JSON.
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        return template;
    }

    //2. Tạo channel Pub/Sub: Tạo = bean riêng, thay vì fix cứng trong redisContainer, dễ bảo trì hơn
    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic("payment-channel");
    }

    //3. Container trung tâm: knoi Redis + đky subcriber và channel
    @Bean
    public RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
                                                        RedisSubscriber subscriber, // Đăng ký trực tiếp
//                                                        MessageListenerAdapter listenerAdapter,//cách 2 dky subcriber
                                                        ChannelTopic topic) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
//        container.addMessageListener(listenerAdapter, topic);//cách 2 dky subcriber
        container.addMessageListener(subscriber, topic);
        log.info("📡 RedisMessageListenerContainer is initialized!");
        return container;
    }
}
