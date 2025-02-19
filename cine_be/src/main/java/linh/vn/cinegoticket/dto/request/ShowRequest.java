package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShowRequest {
    @JsonProperty(value = "cinemaID")
    @NotNull
    @NotBlank
    private String cinemaID;

    @JsonProperty(value = "movieID")
    @NotNull
    private Long movieID;

    @JsonProperty(value = "startTime")
    @NotBlank
    private String startTime;

}
