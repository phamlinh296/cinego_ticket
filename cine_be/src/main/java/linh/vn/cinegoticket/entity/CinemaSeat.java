package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.ESeat;
import linh.vn.cinegoticket.enums.ESeatStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "CinemaSeat")
public class CinemaSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rowIndex")
    private int rowIndex;

    @Column(name = "colIndex")
    private int colIndex;

    @Column(name = "seatType")
    @Enumerated(EnumType.STRING)
    private ESeat seatType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ESeatStatus status;

    @Column(name = "price")
    private double price;

    @Column(name = "name")
    private String name;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    //CinemaSEAT- cinemaHall= N-1
    @ManyToOne
    @JoinColumn(name = "cinemahall_id")
    private CinemaHall cinemaHall;

    public CinemaSeat() {
    }

    //xđ 1 seat thông qua hall, row, col. loại ghế
    public CinemaSeat(CinemaHall cinemaHall, int rowIndexNumber, int colIndexNumber, ESeat type) {
        this.cinemaHall = cinemaHall;
        this.rowIndex = rowIndexNumber;
        this.colIndex = colIndexNumber;
        this.seatType = type;
        this.status = ESeatStatus.AVAILABLE;
        this.name = cinemaHall.getName() + rowIndexNumber + "." + colIndexNumber;

        if (type.equals(ESeat.REGULAR))
            this.price = 10000;
        else
            this.price = 20000;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CinemaHall getCinemaHall() {
        return cinemaHall;
    }

    public void setCinemaHall(CinemaHall cinemaHall) {
        this.cinemaHall = cinemaHall;
    }

    public int getRowIndex() {
        return this.rowIndex;
    }

    public void setRowIndex(int rowIndexNumber) {
        this.rowIndex = rowIndexNumber;
    }

    public int getColIndex() {
        return this.colIndex;
    }

    public void setColIndex(int colIndexNumber) {
        this.colIndex = colIndexNumber;
    }

    public String getSeatType() {
        return this.seatType.name();
    }

    public void setSeatType(ESeat type) {
        this.seatType = type;
        if (type.equals(ESeat.REGULAR))
            this.price = 10000;
        else
            this.price = 20000;
    }


    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public ESeatStatus getStatus() {
        return this.status;
    }

    public void setStatus(ESeatStatus status) {
        this.status = status;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }
}