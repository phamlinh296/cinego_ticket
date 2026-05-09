package linh.vn.spark.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Shared model với cine-be PaymentEvent.
 * Phải implement Serializable vì Spark serialize object để gửi qua mạng giữa các executor.
 * <p>
 * Note: Trong production, nên dùng shared library (module riêng) hoặc Avro/Protobuf schema.
 * Ở đây duplicate để giữ spark-processor độc lập (không phụ thuộc cine-be).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("paymentId")
    private String paymentId;

    @JsonProperty("userId")
    private String userId;

    @JsonProperty("movieId")
    private String movieId;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("deviceIp")
    private String deviceIp;

    @JsonProperty("time")
    private Long time;   // epoch milli — dễ xử lý hơn Instant trong Spark

    @JsonProperty("location")
    private String location;

    @JsonProperty("returnCode")
    private String returnCode;
}