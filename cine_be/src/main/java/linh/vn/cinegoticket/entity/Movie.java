package linh.vn.cinegoticket.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import linh.vn.cinegoticket.dto.request.MovieRequest;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Movie",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"title", "id"})
        })
@Getter
@Setter
public class Movie {


    //    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreationTimestamp
    @Column(name = "CreatedAt", updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "lastUpdated")
    private Date lastUpdated;

    @Column(name = "title")
    private String title;

    @Column(name = "description", length = 3000)
    private String description;

    @Column(name = "durationInMins")
    private int durationInMins;

    @Column(name = "language")
    private String language;

    @Column(name = "releaseDate")
    private String releaseDate;

    @Column(name = "country")
    private String country;

    @Column(name = "image")
    private String image;

    @Column(name = "large_image")
    private String largeImage;

    @Column(name = "trailer")
    private String trailer;

    @Column(name = "actors")
    private String actors;

    //MOVIE - GENRE = N-N
//    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)//nếu tự động lưu
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)//nếu save movie có logic lưu thủ công
    @JoinTable(name = "Movie_Genre",
            joinColumns = {
                    @JoinColumn(name = "movie_id", referencedColumnName = "id")

            },
            inverseJoinColumns = {
                    @JoinColumn(name = "genre_id", referencedColumnName = "id")
            }
    )
    @JsonProperty(value = "genres")
    private List<Genre> genres;
    //MOVIE - COMMENT = 1-N
    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public Movie() {
    }

    public Movie(MovieRequest req) {
        this.title = req.getTitle();
        this.description = req.getDescription();
        this.durationInMins = req.getDurationInMins();
        this.language = req.getLanguage();
        this.releaseDate = req.getReleaseDate();
        this.country = req.getCountry();
        this.image = req.getImage();
        this.largeImage = req.getLargeImage();
    }

    //vì comments là 1 list
    public void addComment(Comment comment) {
        this.comments.add(comment);
    }
    //kbao pthuc ở đây, để sau muốn thêm comment vào tạo list comment,
    // kp tạo lại new list từ đầu, r thêm comment vào list này, r set vào thuộc tính comment của movie nữa
    //mà dùng luôn movie.addComment(comment); là n tự hiểu lưu comment vào list comments kia r.

    public void removeComment(Comment comment) {
        this.comments.remove(comment);
    }
}
