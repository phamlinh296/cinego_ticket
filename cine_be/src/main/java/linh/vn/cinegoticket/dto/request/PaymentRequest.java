package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentRequest {

    @JsonProperty(value = "bookingID")
    @NotNull
    @NotBlank
    private String bookingID;

    @JsonProperty(value = "paymentType")
    @NotNull
    @NotBlank
    private String paymentType;

    public PaymentRequest(String bookingID, String paymentType) {
        this.bookingID = bookingID;
        this.paymentType = paymentType;
    }

}
