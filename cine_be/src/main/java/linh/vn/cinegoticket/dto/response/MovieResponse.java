package linh.vn.cinegoticket.dto.response;

import linh.vn.cinegoticket.entity.Comment;
import linh.vn.cinegoticket.entity.Genre;
import linh.vn.cinegoticket.entity.Movie;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private int durationInMins;
    private String language;
    private String releaseDate;
    private String country;
    private String image;
    private String large_image;
    private String trailer;
    private String actors;
    private List<Genre> genres;
    private List<CommentResponse> comments;

    public MovieResponse(Movie m) {
        this.id = m.getId();
        this.title = m.getTitle();
        this.description = m.getDescription();
        this.language = m.getLanguage();
        this.releaseDate = m.getReleaseDate();
        this.country = m.getCountry();
        this.trailer = m.getTrailer();
        this.actors = m.getActors();
        this.image = m.getImage();
        this.large_image = m.getLargeImage();
        this.genres = m.getGenres();
        this.durationInMins = m.getDurationInMins();
        this.comments = this.convertType(m.getComments());
    }

    public MovieResponse(Long id, String title, String image, String large_image, List<Genre> genre, int durationInMins, List<Comment> comment) {
        this.id = id;
        this.title = title;
        this.image = image;
        this.large_image = large_image;
        this.genres = genre;
        this.durationInMins = durationInMins;
        this.comments = this.convertType(comment);
    }

    public void setDurationInMins(int durationInMins) {
        this.durationInMins = durationInMins;
    }

    private List<CommentResponse> convertType(List<Comment> comments) {
        List<CommentResponse> data = new ArrayList<CommentResponse>();
        for (Comment c : comments)
            data.add(new CommentResponse(c));
        return data;
    }

}
