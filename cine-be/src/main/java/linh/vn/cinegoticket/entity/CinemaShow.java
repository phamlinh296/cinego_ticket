package linh.vn.cinegoticket.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import linh.vn.cinegoticket.utils.DateUtils;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "CinemaShow")
public class CinemaShow {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)//➝ Hibernate tạo UUID có độ dài 36 ký tự.
    @Column(name = "id", unique = true, nullable = false, length = 36, insertable = false)//phải để length =36
    private String id;

    @Column(name = "start_time")
    @NotNull
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    //CinemaShow- CinemaHall= N- 1
    @ManyToOne
    private CinemaHall cinemaHall;

    //CinemaShow- movie= N- 1
    @ManyToOne
    private Movie movie;

    public CinemaShow() {
    }

    public CinemaShow(CinemaHall cinemaHall, Movie movie, LocalDateTime startTime, LocalDateTime endTime) {
        this.cinemaHall = cinemaHall;
        this.movie = movie;
        this.startTime = startTime;
        this.endTime = endTime;
//        this.endTime = startTime.plusMinutes(movie.getDurationInMins()).plusMinutes(10);
    }

    public CinemaShow(CinemaHall cinemaHall, Movie movie, String startTimeString) {
        this.cinemaHall = cinemaHall;
        this.movie = movie;
        this.startTime = DateUtils.convertStringDateToDate(startTimeString, "dd/MM/yyyy HH:mm");
        ;
        this.endTime = startTime.plusMinutes(movie.getDurationInMins()).plusMinutes(10);
    }
}