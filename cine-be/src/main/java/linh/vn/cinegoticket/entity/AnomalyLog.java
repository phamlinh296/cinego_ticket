package linh.vn.cinegoticket.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.AnomalyType;
import linh.vn.cinegoticket.enums.DetectionSource;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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

    /** SPRING = rule-based real-time | SPARK = deep analysis window-based */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(10) NOT NULL DEFAULT 'SPRING'")
    private DetectionSource source;

    // composite risk score (0.0 - 1.0)
    private double riskScore;

    // store amount for easier analytics
    private Double amount;

    @Column(length = 1024)
    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
