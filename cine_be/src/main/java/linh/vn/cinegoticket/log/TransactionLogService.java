package linh.vn.cinegoticket.log;


import linh.vn.cinegoticket.kafka.TransactionLogProducer;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TransactionLogService {

    //1. nếu lưu trực tiếp MongoDB
//    private final TransactionLogRepository logRepository;
//
//    public TransactionLogService(TransactionLogRepository logRepository) {
//        this.logRepository = logRepository;
//    }
//
//    public TransactionLog saveLog(TransactionLog log) {
//        return logRepository.save(log);
//    }
//
//    public List<TransactionLog> getAllLogs() {
//        return logRepository.findAll();
//    }

    //2. nếu dùng Kafka
    private final TransactionLogProducer logProducer;

    public TransactionLogService(TransactionLogProducer logProducer) {
        this.logProducer = logProducer;
    }

    public void saveLog(TransactionLog log) {
        logProducer.sendLog(log); // Gửi log qua Kafka thay vì lưu trực tiếp
    }
}
