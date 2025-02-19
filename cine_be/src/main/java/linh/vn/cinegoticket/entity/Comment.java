package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "Comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, length = 36, insertable = false)
    private String id;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)//k dc null, k dc update
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    @NotNull
    @Column(name = "rated")
    private int rated;

    @NotBlank
    @NotNull
    @Column(name = "comment")
    private String comment;

    @NotNull
    @Column(name = "liked")
    private int liked;

    @NotNull
    @Column(name = "disliked")
    private int disliked;

    //many comment - 1 movie
    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    //many comment - 1 user
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Comment() {
    }

    public Comment(Movie m, User u, int r, String c) {
        this.movie = m;
        this.user = u;
        this.rated = r;
        this.comment = c;
        this.liked = 0;
        this.disliked = 0;
    }
}
