package linh.vn.cinegoticket.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RedisPaymentHistoryService {

    private static final String KEY_PREFIX = "recent:payments:";
    private static final int MAX_HISTORY = 20;

    private final StringRedisTemplate redisTemplate;

    public RedisPaymentHistoryService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void push(String userId, double amount) {
        String key = KEY_PREFIX + userId;
        // LPUSH (most recent first)
        redisTemplate.opsForList().leftPush(key, Double.toString(amount));
        // trim to keep MAX_HISTORY
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY - 1);
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    public List<Double> getHistory(String userId) {
        String key = KEY_PREFIX + userId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) return List.of();
        return raw.stream()
                .map(s -> {
                    try { return Double.parseDouble(s); }
                    catch (Exception ex) { return null; }
                })
                .filter(d -> d != null)
                .collect(Collectors.toList());
    }
}
