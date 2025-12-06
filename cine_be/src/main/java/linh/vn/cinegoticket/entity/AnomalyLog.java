package linh.vn.cinegoticket.entity;

import jakarta.persistence.*;
import linh.vn.cinegoticket.enums.AnomalyType;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "anomaly_logs")
@Data
public class AnomalyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String paymentId;
    private String userId;

    @Enumerated(EnumType.STRING)
    private AnomalyType type;

    private double riskScore;

    private Date timestamp;
}
