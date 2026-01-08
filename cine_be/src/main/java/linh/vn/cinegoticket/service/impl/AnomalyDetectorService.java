package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.PaymentEvent;
import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.enums.AnomalyType;
import linh.vn.cinegoticket.redis.RedisPaymentHistoryService;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import linh.vn.cinegoticket.service.impl.ZScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyDetectorService {

    private final AnomalyLogRepository anomalyLogRepository;
    private final RedisPaymentHistoryService redisPaymentHistoryService;
    private final ZScoreService zScoreService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // parameters / thresholds — could be externalized
    private final long FAST_TX_WINDOW_MS = 30_000; // 30s
    private final int FAST_TX_COUNT = 3;
    private final double HIGH_AMOUNT_THRESHOLD = 1_000_000d;

    // in-memory to track recent timestamps per user (simple)
    private final java.util.concurrent.ConcurrentMap<String, java.util.List<Long>> recentTimestamps = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentMap<String, String> lastIp = new java.util.concurrent.ConcurrentHashMap<>();

    public void analyze(PaymentEvent evt) {
        List<AnomalyLog> detected = new ArrayList<>();

        // Rule: multiple fast tx
        if (isMultipleFast(evt)) {
            detected.add(buildLog(evt, AnomalyType.MULTIPLE_FAST_TRANSACTION, 0.8, "Multiple fast transactions"));
        }

        // Rule: failed but charged (example: status FAILED but returnCode == "0")
        if ("FAILED".equalsIgnoreCase(evt.getStatus()) && "0".equals(evt.getReturnCode())) {
            detected.add(buildLog(evt, AnomalyType.FAILED_BUT_CHARGED, 0.95, "Failed but gateway return_code == 0"));
        }

        // Rule: device/ip change
        if (isDeviceChange(evt)) {
            detected.add(buildLog(evt, AnomalyType.DEVICE_CHANGE, 0.6, "Device/IP changed"));
        }

        // Rule: fixed high amount
        if (evt.getAmount() >= HIGH_AMOUNT_THRESHOLD) {
            detected.add(buildLog(evt, AnomalyType.HIGH_AMOUNT, 0.9, "High fixed threshold"));
        }

        // Rule: Z-score amount anomaly
        List<Double> history = redisPaymentHistoryService.getHistory(evt.getUserId());
        if (zScoreService.isOutlier(evt.getAmount(), history)) {
            detected.add(buildLog(evt, AnomalyType.AMOUNT_ZSCORE, 0.92, "Z-score outlier"));
        }

        // If any anomaly detected -> save and publish anomaly-event
        for (AnomalyLog a : detected) {
            anomalyLogRepository.save(a);
            kafkaTemplate.send("anomaly-events", a);
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
        l.setRiskScore(score);
        l.setAmount(evt.getAmount());
        l.setDescription(desc);
        l.setCreatedAt(LocalDateTime.now());
        return l;
    }

    private boolean isMultipleFast(PaymentEvent evt) {
        if (evt.getUserId() == null) return false;
        long now = evt.getTime() != null ? evt.getTime().getTime() : System.currentTimeMillis();
        recentTimestamps.putIfAbsent(evt.getUserId(), new ArrayList<>());
        List<Long> list = recentTimestamps.get(evt.getUserId());
        synchronized (list) {
            list.add(now);
            // remove older than window
            list.removeIf(ts -> now - ts > FAST_TX_WINDOW_MS);
            return list.size() >= FAST_TX_COUNT;
        }
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
