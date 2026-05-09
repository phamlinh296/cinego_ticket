package linh.vn.spark.sink;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;

/**
 * RedisSink: ghi aggregated data từ Spark vào Redis để serve dashboard realtime.
 * <p>
 * Dùng Jedis (không dùng Spring RedisTemplate vì đây là plain Java, không có Spring context).
 * <p>
 * ═══════════════════════════════════════════════════
 * Interviewer question: "Tại sao Spark ghi vào Redis?"
 * ═══════════════════════════════════════════════════
 * Spark tính toán window aggregation (1h sliding window),
 * kết quả (revenue/hour, top movies) được push vào Redis.
 * Dashboard API (Spring Boot) chỉ cần HGETALL từ Redis → sub-ms response.
 * Tách biệt concerns: Spark lo tính toán, Redis lo serving.
 * <p>
 * ═══════════════════════════════════════════════════
 * JedisPool trong Spark Executor:
 * ═══════════════════════════════════════════════════
 * Mỗi executor có 1 JedisPool riêng (static instance).
 * Dùng static để tái sử dụng pool across batches trong cùng executor.
 * Khác với HBase Connection — Jedis pool nhẹ hơn, an toàn hơn để giữ lâu.
 */
@Slf4j
public class RedisSink implements Serializable {

    private static final long serialVersionUID = 1L;

    private static volatile JedisPool pool;

    private final String redisHost;
    private final int redisPort;

    public RedisSink(String redisHost, int redisPort) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
    }

    private JedisPool getPool() {
        if (pool == null) {
            synchronized (RedisSink.class) {
                if (pool == null) {
                    JedisPoolConfig config = new JedisPoolConfig();
                    config.setMaxTotal(10);
                    config.setMaxIdle(5);
                    config.setMinIdle(2);
                    config.setTestOnBorrow(true);
                    pool = new JedisPool(config, redisHost, redisPort,
                            2000 /* connection timeout ms */);
                    log.info("[RedisSink] JedisPool created: {}:{}", redisHost, redisPort);
                }
            }
        }
        return pool;
    }

    /**
     * Cập nhật revenue theo giờ (sliding window result).
     * <p>
     * Key: revenue:{date}:{hour}   e.g. "revenue:2026-04-19:14"
     * Value: tổng revenue (VND)
     * TTL: 48 giờ (dashboard chỉ cần 24h gần nhất)
     */
    public void updateRevenueByHour(String date, int hour, double revenue) {
        String key = "spark:revenue:" + date + ":" + String.format("%02d", hour);
        try (Jedis jedis = getPool().getResource()) {
            jedis.incrByFloat(key, revenue);
            jedis.expire(key, 48 * 3600L);
            log.debug("[RedisSink] revenue updated: key={} +{}", key, revenue);
        } catch (Exception e) {
            log.error("[RedisSink] Redis write failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Cập nhật top movies leaderboard (sorted set).
     * <p>
     * Key: spark:top_movies:{date}
     * Score: tổng số transaction
     * <p>
     * Dashboard query: ZREVRANGE spark:top_movies:2026-04-19 0 9 WITHSCORES → top 10
     */
    public void updateTopMovies(String date, String movieId, double score) {
        String key = "spark:top_movies:" + date;
        try (Jedis jedis = getPool().getResource()) {
            jedis.zincrby(key, score, movieId);
            jedis.expire(key, 48 * 3600L);
            log.debug("[RedisSink] top_movies updated: key={} movie={} +{}", key, movieId, score);
        } catch (Exception e) {
            log.error("[RedisSink] Redis ZADD failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Cập nhật user stats: avg amount, tx count.
     * <p>
     * Key: spark:user_stats:{userId}
     * Hash fields: tx_count, total_amount, avg_amount
     */
    public void updateUserStats(String userId, double amount) {
        String key = "spark:user_stats:" + userId;
        try (Jedis jedis = getPool().getResource()) {
            jedis.hincrBy(key, "tx_count", 1);
            jedis.hincrByFloat(key, "total_amount", amount);

            // Tính avg_amount từ 2 fields trên
            String countStr = jedis.hget(key, "tx_count");
            String totalStr = jedis.hget(key, "total_amount");
            if (countStr != null && totalStr != null) {
                double avg = Double.parseDouble(totalStr) / Long.parseLong(countStr);
                jedis.hset(key, "avg_amount", String.valueOf(avg));
            }
            jedis.expire(key, 7 * 24 * 3600L); // 7 ngày
        } catch (Exception e) {
            log.error("[RedisSink] User stats update failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish fraud alert count vào Redis counter.
     * Dashboard có thể subscribe để show real-time alert badge.
     */
    public void incrementFraudCount(String date) {
        String key = "spark:fraud_count:" + date;
        try (Jedis jedis = getPool().getResource()) {
            jedis.incr(key);
            jedis.expire(key, 48 * 3600L);
        } catch (Exception e) {
            log.error("[RedisSink] Fraud count update failed: {}", e.getMessage(), e);
        }
    }
}