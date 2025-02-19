package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.CinemaHall;
import linh.vn.cinegoticket.entity.CinemaSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CinemaSeatRepository extends JpaRepository<CinemaSeat, Long> {
    List<CinemaSeat> findByCinemaHall(CinemaHall hall);

    List<CinemaSeat> findByCinemaHallId(String hallID);

    Optional<CinemaSeat> findByCinemaHallIdAndRowIndexAndColIndex(String hallID, int row, int col);

    void deleteAllByCinemaHallId(String hallID);


    void deleteAllByCinemaHall_Id(String hallID);
}
