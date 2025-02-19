package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CommentUpdateRequest {

    @NotNull
    @NotBlank
    @JsonProperty("comment")
    private String comment;

    @NotNull
    @JsonProperty("rating")
    private int ratingStars;

    public CommentUpdateRequest() {
    }
}
