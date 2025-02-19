package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class HashRequest {
    @JsonProperty(value = "bookingID")
    @NotNull
    @NotBlank
    private String bookingID;

    @JsonProperty(value = "cardID")
    @NotNull
    @NotBlank
    private String cardID;

    @JsonProperty(value = "cardName")
    @NotNull
    @NotBlank
    private String cardName;

    @JsonProperty(value = "CVCnumber")
    @NotNull
    private int CVCnumber;

    public HashRequest(String bookingID, String cardID, String cardName, int CVCnumber) {
        this.bookingID = bookingID;
        this.cardID = cardID;
        this.cardName = cardName;
        this.CVCnumber = CVCnumber;
    }

}
