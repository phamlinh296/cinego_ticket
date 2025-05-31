package linh.vn.cinegoticket.log;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionLogRepository extends MongoRepository<TransactionLog, String> {
}
