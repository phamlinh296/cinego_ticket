package linh.vn.cinegoticket.repository;

import jakarta.transaction.Transactional;
import linh.vn.cinegoticket.entity.ShowSeat;
import linh.vn.cinegoticket.enums.ESeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, String> {
    int countByShowIdAndStatus(String show_id, ESeatStatus status);

    List<ShowSeat> findByShowId(String showId);

    void deleteAllByShowId(String show_id);

    Optional<ShowSeat> findByIdAndShowId(String id, String showId);

}
