package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BookingRequest {

    @JsonProperty(value = "seats_id")
    @NotNull
    List<String> seatsId;

    @JsonProperty(value = "show_id")
    @NotNull
    @NotBlank
    String showId;

    public List<String> getSeatsId() {
        return this.seatsId;
    }

    public String getShowId() {
        return this.showId;
    }
}
