package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.ESeatStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "ShowSeat")
public class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, length = 36, insertable = false)
    private String id;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ESeatStatus status;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    //SHOWSEAT - SHOW= N- 1
    @ManyToOne
    private CinemaShow show;

    //SHOWSEAT - SEAT= N- 1
    @ManyToOne
    private CinemaSeat cinemaSeat;

    public ShowSeat() {
    }

    public ShowSeat(CinemaShow show, CinemaSeat cinemaSeat, ESeatStatus status) {
        this.show = show;
        this.cinemaSeat = cinemaSeat;
        this.status = status;
    }
}