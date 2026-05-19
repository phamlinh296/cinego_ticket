package linh.vn.cinegoticket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StringRedisTemplate redisTemplate;

    @GetMapping("/revenue")
    public String getRevenue(
            @RequestParam String date,
            @RequestParam int hour
    ) {
        // HIỆN TẠI (key cũ từ AnalyticsProcessor)
        // date: 2026-03-27, hour: 22 → key: revenue:2026-03-27:22 → value: 5,000,000
//        String key = "revenue:" + date + ":" + hour;

        // SAU KHI DÙNG SPARK (RedisSink dùng key khác)
        String key = String.format("spark:revenue:%s:%02d", date, hour);
        return redisTemplate.opsForValue().get(key);
    }

    @GetMapping("/top-movies")
    public Set<String> getTopMovies(
            @RequestParam String date
    ) {
//        String key = "top_movies:" + date;
        String key = "spark:top_movies:" + date;
        return redisTemplate.opsForZSet().reverseRange(key, 0, 4);
    }
}