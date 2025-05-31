package linh.vn.cinegoticket.log;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class TransactionLogController {

    private final TransactionLogService logService;

    public TransactionLogController(TransactionLogService logService) {
        this.logService = logService;
    }

    //1. nếu lưu trực tiếp vào Mongodb
//    @PostMapping
//    public TransactionLog createLog(@RequestBody Map<String, Object> body) {
//        String transactionId = (String) body.get("transactionId");
//        String userId = (String) body.get("userId");
//        String action = (String) body.get("action");
//        String status = (String) body.get("status");
//        Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");
//
//        TransactionLog log = new TransactionLog(transactionId, userId, action, LocalDateTime.now(), status, metadata);
//        return logService.saveLog(log); // lưu trực tiếp vào MongoDB
//    }

//
//    @GetMapping
//    public List<TransactionLog> getLogs() {
//        return logService.getAllLogs();
//    }

    //2. nếu dùng kafka, test kafka
    @PostMapping
    public String createLog(@RequestBody Map<String, Object> body) {
        String transactionId = (String) body.get("transactionId");
        String userId = (String) body.get("userId");
        String action = (String) body.get("action");
        String status = (String) body.get("status");
        Map<String, Object> metadata = (Map<String, Object>) body.get("metadata");

        TransactionLog log = new TransactionLog(transactionId, userId, action, LocalDateTime.now(), status, metadata);
        logService.saveLog(log); // gửi vào Kafka
        return "Sent to Kafka!";
    }


}

