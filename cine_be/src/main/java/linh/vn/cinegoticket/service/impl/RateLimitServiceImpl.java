package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate.limit.login.max-requests}")
    private int maxRequests;

    @Value("${rate.limit.login.duration-seconds}")
    private int duration;

    @Override
    public void checkLoginRateLimit(String key) {
        String redisKey = "rate_limit:login:" + key;

        Long count = redisTemplate.opsForValue().increment(redisKey);//tăng count lên 1, nếu key chưa tồn tại thì tạo mới với giá trị 1

        if (count == 1) {
            redisTemplate.expire(redisKey, duration, TimeUnit.SECONDS); //Chỉ set TTL khi request đầu tiên tới.
        }

        if (count != null && count > maxRequests) {
            throw new AppException("Too many login attempts. Please try again later.", ErrorCode.TOO_MANY_REQUESTS
            );
        }
    }
}
