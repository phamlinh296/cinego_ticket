package linh.vn.cinegoticket.repository;

import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import linh.vn.cinegoticket.entity.ShowSeat;
import linh.vn.cinegoticket.enums.ESeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Transactional
@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, String> {
    int countByShowIdAndStatus(String show_id, ESeatStatus status);

    List<ShowSeat> findByShowId(String showId);

    void deleteAllByShowId(String show_id);

    //pessimistic lock ngay khi chọn ghế
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ShowSeat> findByIdAndShowId(String id, String showId);

    @Modifying
    @Query("""
        DELETE FROM ShowSeat ss
        WHERE ss.cinemaSeat.id IN (
            SELECT cs.id FROM CinemaSeat cs WHERE cs.cinemaHall.id = :hallId
        )
    """)
    void deleteAllByHallId(@Param("hallId") String hallId);

}
