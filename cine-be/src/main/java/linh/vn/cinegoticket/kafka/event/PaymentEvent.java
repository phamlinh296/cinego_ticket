package linh.vn.cinegoticket.kafka.event;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class PaymentEvent {
    private String paymentId;
    private String userId;
    private String movieId;
    private double amount;
    private String deviceIp;
//    private Date time; //java.util.Date không thread-safe, deprecated từ Java 8. Thay bằng java.time.Instant hoặc LocalDateTime
//    private LocalDateTime time; // nếu dùng LocalDateTime thì phải convert sang Instant khi serialize để đảm bảo timezone
    private Instant time;
    private String status;
    private String location;

    private String returnCode; // optional, VNPay return code if available
}

//Instant = thời điểm tuyệt đối trên timeline, không phụ thuộc timezone.
// Khi Kafka consumer chạy ở server khác timezone, hoặc sau này scale ra multi-region, LocalDateTime sẽ gây bug âm thầm.
// Instant là chuẩn cho event-driven system.