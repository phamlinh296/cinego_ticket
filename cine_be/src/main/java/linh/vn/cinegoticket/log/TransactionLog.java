package linh.vn.cinegoticket.log;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@Document(collection = "transaction_logs")
public class TransactionLog {
    @Id
    private String id;

    private String transactionId;
    private String userId;
    private String action;
    private LocalDateTime timestamp;
    private String status;
    private Map<String, Object> metadata;

    public TransactionLog() {}

    public TransactionLog(String transactionId, String userId, String action, LocalDateTime timestamp, String status, Map<String, Object> metadata) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
        this.status = status;
        this.metadata = metadata;
    }

}