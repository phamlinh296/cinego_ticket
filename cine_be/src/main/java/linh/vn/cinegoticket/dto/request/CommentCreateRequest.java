package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CommentCreateRequest {

    @NotNull
    @JsonProperty("movie_id")
    private long movieId;

    @NotNull
    @NotBlank
    @JsonProperty("comment")
    private String comment;

    @NotNull
    @JsonProperty("rating")
    private int ratedStars;

    public CommentCreateRequest() {
    }
}
