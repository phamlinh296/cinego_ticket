package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.CinemaShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CinemaShowRepository extends JpaRepository<CinemaShow, String> {
    @Query(value = "SELECT s FROM CinemaShow s WHERE s.id <> :currentShowID " + //loại chính nó rồi ms tìm các show trong time này
            "AND ((:startTime >= s.startTime AND :startTime <= s.endTime) OR (:endTime >= s.startTime AND :endTime <= s.endTime)) AND s.cinemaHall.id = :hallID")
    List<CinemaShow> findConflictingShows(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("hallID") String hallID,
                                          @Param("currentShowID") String currentShowID);

    List<CinemaShow> findByCinemaHallId(String hallId);

    List<CinemaShow> findByMovieId(Long movieId);

    @Query(value = "SELECT s FROM CinemaShow s WHERE s.cinemaHall.id = :hallId AND s.movie.id = :movieId AND s.startTime = :startTime")
    CinemaShow findByHallIdAndMovieId(@Param("hallId") String hallId, @Param("movieId") Long movieId, @Param("startTime") LocalDateTime startTime);
}
