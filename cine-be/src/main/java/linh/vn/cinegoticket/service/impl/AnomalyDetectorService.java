package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.kafka.event.PaymentEvent;
import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.enums.AnomalyType;
import linh.vn.cinegoticket.enums.DetectionSource;
import linh.vn.cinegoticket.redis.RedisPaymentHistoryService;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyDetectorService {

    private final AnomalyLogRepository anomalyLogRepository;
    private final RedisPaymentHistoryService redisPaymentHistoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // parameters / thresholds — could be externalized
    private final long FAST_TX_WINDOW_MS = 30_000; // 30s
    private final int FAST_TX_COUNT = 3;
    private final double HIGH_AMOUNT_THRESHOLD = 1_000_000d;

    private final ConcurrentHashMap<String, List<Long>> recentTimestamps = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> lastIp = new ConcurrentHashMap<>();

    // Dedup: suppress duplicate alert của cùng user+type trong vòng FAST_TX_WINDOW_MS
    // Key: "userId:ANOMALY_TYPE", value: epoch ms của lần alert gần nhất
    private final ConcurrentHashMap<String, Long> alertCooldown = new ConcurrentHashMap<>();

    public void analyze(PaymentEvent evt) {
        List<AnomalyLog> detected = new ArrayList<>();
        long now = Instant.now().toEpochMilli();

        // Rule: multiple fast tx — chỉ alert lần đầu trong 30s window, không lặp lại cho mỗi event tiếp theo
        if (isMultipleFast(evt) && !isInCooldown(evt.getUserId(), AnomalyType.MULTIPLE_FAST_TRANSACTION, now)) {
            detected.add(buildLog(evt, AnomalyType.MULTIPLE_FAST_TRANSACTION, 0.8, "Multiple fast transactions"));
        }

        // Rule: failed but charged — per event, không cần cooldown
        if ("FAILED".equalsIgnoreCase(evt.getStatus()) && "0".equals(evt.getReturnCode())) {
            detected.add(buildLog(evt, AnomalyType.FAILED_BUT_CHARGED, 0.95, "Failed but gateway return_code == 0"));
        }

        // Rule: device/ip change — per event, không cần cooldown
        if (isDeviceChange(evt)) {
            detected.add(buildLog(evt, AnomalyType.DEVICE_CHANGE, 0.6, "Device/IP changed"));
        }

        // Rule: fixed high amount — per event, không cần cooldown
        if (evt.getAmount() >= HIGH_AMOUNT_THRESHOLD) {
            detected.add(buildLog(evt, AnomalyType.HIGH_AMOUNT, 0.9, "High fixed threshold"));
        }

        // Save + publish, ghi cooldown sau khi save thành công
        for (AnomalyLog a : detected) {
            anomalyLogRepository.save(a);
            kafkaTemplate.send("anomaly-events", a);
            alertCooldown.put(evt.getUserId() + ":" + a.getType(), now);
            log.info("Saved anomaly {} for payment {}", a.getType(), a.getPaymentId());
        }

        // Regardless, push amount into redis history for future checks
        if (evt.getUserId() != null) {
            redisPaymentHistoryService.push(evt.getUserId(), evt.getAmount());
        }
    }

    private AnomalyLog buildLog(PaymentEvent evt, AnomalyType type, double score, String desc) {
        AnomalyLog l = new AnomalyLog();
        l.setPaymentId(evt.getPaymentId());
        l.setUserId(evt.getUserId());
        l.setType(type);
        l.setSource(DetectionSource.SPRING);
        l.setRiskScore(score);
        l.setAmount(evt.getAmount());
        l.setDescription(desc);
        l.setCreatedAt(LocalDateTime.now());
        return l;
    }

    private boolean isMultipleFast(PaymentEvent evt) {
        if (evt.getUserId() == null) return false;
        long now = evt.getTime() != null
                ? evt.getTime().toEpochMilli()
                : Instant.now().toEpochMilli();
        List<Long> list = recentTimestamps.computeIfAbsent(evt.getUserId(), k -> new ArrayList<>());
        synchronized (list) {
            list.add(now);
            list.removeIf(ts -> now - ts > FAST_TX_WINDOW_MS);
            return list.size() >= FAST_TX_COUNT;
        }
    }

    private boolean isInCooldown(String userId, AnomalyType type, long now) {
        Long last = alertCooldown.get(userId + ":" + type);
        return last != null && (now - last) < FAST_TX_WINDOW_MS;
    }

    @Scheduled(fixedRate = 3_600_000)
    public void cleanupStaleUsers() {
        long cutoff = Instant.now().toEpochMilli() - FAST_TX_WINDOW_MS;
        recentTimestamps.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                return entry.getValue().stream().allMatch(ts -> ts < cutoff);
            }
        });
        alertCooldown.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    private boolean isDeviceChange(PaymentEvent evt) {
        if (evt.getUserId() == null) return false;
        String prev = lastIp.putIfAbsent(evt.getUserId(), evt.getDeviceIp());
        if (prev == null) return false;
        boolean changed = !prev.equals(evt.getDeviceIp());
        lastIp.put(evt.getUserId(), evt.getDeviceIp());
        return changed;
    }
}
