package linh.vn.cinegoticket.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RedisService {

    private static final String RECENT_PAYMENTS_KEY = "recent:payments:"; // + userId
    private static final int MAX_HISTORY = 20;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // push amount (String) và trim list
    public void pushPaymentAmount(String userId, double amount) {
        String key = RECENT_PAYMENTS_KEY + userId;
        // LPUSH then LTRIM to keep latest MAX_HISTORY elements
        redisTemplate.opsForList().leftPush(key, Double.toString(amount));
        redisTemplate.opsForList().trim(key, 0, MAX_HISTORY - 1);
        // optionally set TTL
        redisTemplate.expire(key, Duration.ofDays(30));
    }

    // get recent payments as List<Double>
    public List<Double> getRecentPayments(String userId) {
        String key = RECENT_PAYMENTS_KEY + userId;
        List<String> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null) return Collections.emptyList();
        return raw.stream()
                  .map(s -> {
                      try { return Double.parseDouble(s); }
                      catch (Exception e) { return null; }
                  })
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList());
    }
}
