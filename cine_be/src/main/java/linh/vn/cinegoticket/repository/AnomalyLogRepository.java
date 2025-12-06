package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.AnomalyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnomalyLogRepository extends JpaRepository<AnomalyLog, String> {}
