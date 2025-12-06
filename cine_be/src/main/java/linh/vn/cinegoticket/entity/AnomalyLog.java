package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.AnomalyType;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "anomaly_logs", indexes = {
        @Index(name = "idx_anomaly_type", columnList = "type"),
        @Index(name = "idx_anomaly_timestamp", columnList = "created_at")
})
@Data
public class AnomalyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    private AnomalyType type;

    // composite risk score (0.0 - 1.0)
    private double riskScore;

    // store amount for easier analytics
    private Double amount;

    @Column(length = 1024)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
