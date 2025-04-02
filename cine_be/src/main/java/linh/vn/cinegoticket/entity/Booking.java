package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import linh.vn.cinegoticket.enums.BookingStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "Booking",
        indexes = {
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_userid", columnList = "user_id"),
                @Index(name = "idx_bookingid_userid", columnList = "id, user_id")//tên cot trong db, kp ten trường
        })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, length = 36, insertable = false)
    private String id;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private BookingStatus status;

    //booking - showseat= N-N
    @ManyToMany(fetch = FetchType.EAGER)
    private List<ShowSeat> seats;

    //booking - user= N-1
    @ManyToOne
    @NotNull
    private User user;

    //booking- cinemaShow = N-1
    @ManyToOne
    @NotNull
    private CinemaShow show;

    public Booking() {
    }

    public Booking(Booking booking) {
        this.user = booking.getUser();
        this.show = booking.getShow();
        this.seats = booking.getSeats();
        this.status = BookingStatus.PENDING;
    }

    public Booking(User user, CinemaShow show, List<ShowSeat> seats) {
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.status = BookingStatus.PENDING;
    }

    public void addSeat(ShowSeat seat) {
        this.seats.add(seat);
    }

    public void removeSeat(ShowSeat seat) {
        this.seats.remove(seat);
    }

    public boolean isEmptySeats() {
        return this.seats.isEmpty();
    }

    public List<String> getNameOfSeats() {
        List<String> names = new ArrayList<>();
        for (ShowSeat seat : this.seats)
            names.add(seat.getCinemaSeat().getName());
        return names;
    }

    public double getPriceFromListSeats() {
        double res = 0;
        for (ShowSeat seat : this.seats)
            res += seat.getCinemaSeat().getPrice();
        return res;
    }
}








