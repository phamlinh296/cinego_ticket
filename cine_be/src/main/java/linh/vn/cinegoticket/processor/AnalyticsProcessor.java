package linh.vn.cinegoticket.processor;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsProcessor {

    private final StringRedisTemplate redisTemplate;
    private final AtomicLong counter = new AtomicLong();

    //realtime, mỗi event 1 lần: update Redis, aggregation theo giờ
    public void process(PaymentEvent event) {

        long start = System.currentTimeMillis();

        try {
            // 1. Idempotency - avoid duplicate processing
            String processedKey = "processed:" + event.getPaymentId();
            Boolean isNew = redisTemplate.opsForValue()
                    .setIfAbsent(processedKey, "1", Duration.ofHours(1)); //nếu key chưa tồn tại thì set (key, value, TTL) và trả về true, nếu đã tồn tại thì trả về false

            if (Boolean.FALSE.equals(isNew)) {
                log.warn("Duplicate event {}", event.getPaymentId());
                return;
            }

            // 2. Event time (FIX)
            LocalDateTime eventTime = event.getTime()// dam bao dung time event occur (luc user nhan thanh toan) thay vi time processing (khi kafka nhan even de xly)
//                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            String date = eventTime.toLocalDate().toString();
            String hour = String.valueOf(eventTime.getHour());

            double amount = event.getAmount();
            String movieId = event.getMovieId();

            // 3. Revenue
            String revenueKey = "revenue:" + date + ":" + hour;
            redisTemplate.opsForValue().increment(revenueKey, amount); //sum doanh thu theo giờ: revenue:2026-03-27:22 → 5,000,000

            // 4. Top movies
            String topMoviesKey = "top_movies:" + date;
            redisTemplate.opsForZSet().incrementScore(topMoviesKey, movieId, 1);//lấy key để lấy danh sách, xong lấy member movieid (chưa co thi tao) và lấy value của phim đó tăng lên 1;
            // r edis sẽ tự động sắp xếp theo value giảm dần, khi lấy top 10 thì sẽ lấy 10 phim có doanh thu cao nhất trong ngày đó
            //  ttl
            redisTemplate.expire(revenueKey, Duration.ofDays(1));
            redisTemplate.expire(topMoviesKey, Duration.ofDays(1));

            // METRICS
            long count = counter.incrementAndGet();
            long latency = System.currentTimeMillis() - start; //đo latency - detect issue sớm

            log.info("Processed {} | total={} | latency={}ms",
                    event.getPaymentId(), count, latency);

        } catch (Exception e) {
            log.error("Error processing {}", event.getPaymentId(), e);
            throw e; // trigger retry + DLQ
        }
    }
}