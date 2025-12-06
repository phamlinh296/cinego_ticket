package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.dto.response.DateCountDto;
import linh.vn.cinegoticket.entity.AnomalyLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface AnomalyLogRepository extends JpaRepository<AnomalyLog, String> {
    // Count per date since fromDate (use LocalDateTime start-of-day)
    @Query("SELECT new linh.vn.cinegoticket.dto.response.DateCountDto(FUNCTION('DATE_FORMAT', a.createdAt, '%Y-%m-%d'), COUNT(a)) " +
            "FROM AnomalyLog a WHERE a.createdAt >= :fromDate GROUP BY FUNCTION('DATE_FORMAT', a.createdAt, '%Y-%m-%d') ORDER BY FUNCTION('DATE_FORMAT', a.createdAt, '%Y-%m-%d')")
    List<DateCountDto> countByDate(@Param("fromDate") LocalDateTime fromDate);

    // Count by type
    @Query("SELECT a.type, COUNT(a) FROM AnomalyLog a GROUP BY a.type")
    List<Object[]> countByType();
}
