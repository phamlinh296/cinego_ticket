package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "Payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", unique = true, nullable = false, length = 36, insertable = false)
    private String id;

    @Column(name = "amount")
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    @CreationTimestamp
    @Column(name = "create_at", nullable = false, updatable = false)
    private Date createAt;

    @UpdateTimestamp
    @Column(name = "update_at", nullable = true, updatable = true)
    private Date updateAt;

    //PAYMENT- BOOKING =1-1
    @OneToOne
    private Booking booking;

    public Payment() {
    }

    public Payment(Booking booking, double amount) {
        this.booking = booking;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public void canclePayment() {
        this.status = PaymentStatus.CANCLED;
    }

    public void returnPayment() {
        this.status = PaymentStatus.RETURNED;
    }
}