package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.PaymentEvent;
import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.enums.AnomalyType;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import linh.vn.cinegoticket.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnomalyDetectionService {

    @Autowired
    private AnomalyLogRepository anomalyLogRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    private Map<String, List<Date>> userPaymentHistory = new ConcurrentHashMap<>();
    private Map<String, String> userIpRecord = new ConcurrentHashMap<>();


    public void detect(PaymentEvent event) {

        List<AnomalyLog> anomalies = new ArrayList<>();

        // RULE 1: Nhiều giao dịch trong 30 giây
        if (isMultipleFastTransaction(event)) {
            anomalies.add(createAnomaly(event, AnomalyType.MULTIPLE_FAST_TRANSACTION, 0.85));
        }

        // RULE 2: Thanh toán thất bại nhưng return_code thành công
        if (isFailButCharged(event)) {
            anomalies.add(createAnomaly(event, AnomalyType.FAILED_BUT_CHARGED, 0.9));
        }

        // RULE 3: Đổi thiết bị/IP
        if (isDeviceChange(event)) {
            anomalies.add(createAnomaly(event, AnomalyType.DEVICE_CHANGE, 0.7));
        }

        // RULE 4: Số tiền bất thường
        if (isHighAmount(event)) {
            anomalies.add(createAnomaly(event, AnomalyType.HIGH_AMOUNT, 0.95));
        }

        // SAVE + publish Kafka
        // Khi phát hiện anomaly → Ghi log vào DB + Gửi event đến Kafka topic "anomaly-events"
        for (AnomalyLog log : anomalies) {
            anomalyLogRepository.save(log);
            kafkaTemplate.send("anomaly-events", log);
        }
    }

    //RULE 1: Nhiều giao dịch trong 30s
    private boolean isMultipleFastTransaction(PaymentEvent event) {
        String userId = event.getUserId();
        userPaymentHistory.putIfAbsent(userId, new ArrayList<>());

        List<Date> times = userPaymentHistory.get(userId);
        times.add(event.getTime());

        // giữ danh sách 60s gần nhất
        times.removeIf(t -> event.getTime().getTime() - t.getTime() > 30000);

        return times.size() >= 3;
    }

    //RULE 2: Thanh toán thất bại nhưng return_code thành công
    //trả về failed nhưng VNPay (mô phỏng) cho return_code=0
    private boolean isFailButCharged(PaymentEvent event) {
        return "FAILED".equals(event.getStatus()) && event.getAmount() > 0;
    }

    //RULE 3: thay đổi IP bất thường - So sánh IP mới với IP gần nhất của user:
    private boolean isDeviceChange(PaymentEvent event) {
        String userId = event.getUserId();

        if (!userIpRecord.containsKey(userId)) {
            userIpRecord.put(userId, event.getDeviceIp());
            return false;
        }

        boolean changed = !userIpRecord.get(userId).equals(event.getDeviceIp());
        userIpRecord.put(userId, event.getDeviceIp());

        return changed;
    }

    //RULE 4: thanh toán vượt ngưỡng bất thường
    //Giả sử user chỉ trả 100k–150k nhưng đột nhiên trả >1,000,000:
    private boolean isHighAmount(PaymentEvent event) {
        return event.getAmount() >= 1_000_000;
    }

    private AnomalyLog createAnomaly(PaymentEvent event, AnomalyType type, double riskScore) {
        AnomalyLog log = new AnomalyLog();
        log.setPaymentId(event.getPaymentId());
        log.setUserId(event.getUserId());
        log.setType(type);
        log.setRiskScore(riskScore);
        log.setTimestamp(new Date());
        return log;
    }



}
