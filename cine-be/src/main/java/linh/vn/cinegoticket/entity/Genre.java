package linh.vn.cinegoticket.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "genre",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"genre", "id"})
        })
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @CreationTimestamp//lấy thời gian hiện tại gán vào trường này
    @Column(name = "CreatedAt", updatable = false)
    private Date createdAt;

    @UpdateTimestamp//tự update time khi bản ghi bị change
    @Column(name = "lastUpdated")
    private Date lastUpdated;

    @Column(name = "genre")
    private String genre;

    //genre - movie= N- N
    @ManyToMany(mappedBy = "genres", fetch = FetchType.LAZY)
    @JsonBackReference//k cho chuyển từ đtg java thành json
    private List<Movie> movies;

    public Genre() {
    }

    public Genre(Long id, String genre) {
        this.id = id;
        this.genre = genre;
    }
}
