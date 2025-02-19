package linh.vn.cinegoticket.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MovieRequest {
    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("durationInMins")
    private int durationInMins;

    @JsonProperty("language")
    private String language;

    @JsonProperty("releaseDate")
    private String releaseDate;

    @JsonProperty("country")
    private String country;

    @JsonProperty("image")
    private String image;

    @JsonProperty("large_image")
    private String largeImage;

    @JsonProperty("trailer")
    private String trailer;

    @JsonProperty("actors")
    private String actors;

    @JsonProperty("genre")
    @ManyToMany
    private List<String> genre;
}
