package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import linh.vn.cinegoticket.dto.request.CinemaHallRequest;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Entity
@Table(name = "CinemaHall",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})}
)
public class CinemaHall {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, length = 26, insertable = false)
    private String id;

    @Column(name = "name", nullable = false)
    @NotBlank
    private String name;

    @Column(name = "totalRow", nullable = false)
    @NotNull
    private int totalRow;

    @Column(name = "totalCol", nullable = false)
    @NotNull
    private int totalCol;

    @Column(name = "capacity")
    @NotNull
    private int capacity;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    public CinemaHall() {
    }

    public CinemaHall(CinemaHallRequest cReq) {
        this.name = cReq.getName();
        this.totalCol = cReq.getTotalCol();
        this.totalRow = cReq.getTotalRow();
        this.capacity = this.totalCol * this.totalRow;
    }

    public CinemaHall(String name, int totalRow, int totalCol) {
        this.name = name;
        this.totalCol = totalCol;
        this.totalRow = totalRow;
        this.capacity = this.totalCol * this.totalRow;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getTotalRow() {
        return this.totalRow;
    }

    public void setTotalRow(int total) {
        this.totalRow = total;
        this.setCapacity(this.totalCol * this.totalRow);
    }

    public int getTotalCol() {
        return this.totalCol;
    }

    public void setTotalCol(int total) {
        this.totalCol = total;
        this.setCapacity(this.totalCol * this.totalRow);
    }
}