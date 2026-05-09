package linh.vn.shared.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PaymentEvent — Kafka message contract dùng chung giữa cine-be và spark-processor.
 * <p>
 * ════════════════════════════════════════════════════════
 * Tại sao đặt ở shared-model, không duplicate?
 * ════════════════════════════════════════════════════════
 * Trước đây: cine-be có 1 PaymentEvent, spark-processor có 1 PaymentEvent riêng.
 * → Nếu thêm field `seatIds` → phải nhớ sửa cả 2 chỗ.
 * → Lệch schema → Kafka deserialization fail, bug khó tìm.
 * <p>
 * Sau: cả hai module đều import từ shared-model.
 * → Thêm field 1 lần → compile error nếu ai đó quên handle → safe.
 * <p>
 * ════════════════════════════════════════════════════════
 * Serializable: bắt buộc vì Spark serialize object để
 * gửi qua mạng giữa Driver và Executor.
 * ════════════════════════════════════════════════════════
 *
 * @JsonIgnoreProperties(ignoreUnknown = true): forward-compatible —
 * nếu producer thêm field mới, consumer cũ không crash.
 */

//dufung shared model này để tránh duplicate với cine-be, spark-processor
// nhung hien tại cu dung cũng ko sao vì 2 module này chưa có nhiều field, sau này nếu có nhiều field thì sẽ move sang shared model
// de day de biet kien truc thoi, sau dung.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("movieId")
    private String movieId;

    /**
     * Số tiền thanh toán (VND).
     */
    @JsonProperty("amount")
    private double amount;

    /**
     * PAID | FAILED | PENDING | CANCELLED
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("deviceIp")
    private String deviceIp;

    /**
     * Thời điểm xảy ra event — epoch milliseconds.
     * Dùng Long thay vì Instant để:
     * 1. Dễ serialize/deserialize JSON (không cần custom deserializer)
     * 2. Spark đọc trực tiếp làm timestamp column: col("time").divide(1000).cast(TimestampType)
     */
    @JsonProperty("time")
    private Long time;

    @JsonProperty("location")
    private String location;

    /**
     * Return code từ payment gateway (VNPay).
     * "0" = success theo VNPay convention.
     * Dùng để detect FAILED_BUT_CHARGED anomaly.
     */
    @JsonProperty("returnCode")
    private String returnCode;
}