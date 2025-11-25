package linh.vn.cinegoticket.repository;

import jakarta.validation.constraints.NotBlank;
import linh.vn.cinegoticket.entity.Movie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends
        PagingAndSortingRepository<Movie, Long>,
        JpaRepository<Movie, Long> {
    boolean existsByTitle(@NotBlank String title);

    List<Movie> findByTitleContaining(String title, Pageable pages);

    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.genre LIKE CONCAT('%', :keyword, '%')")
    List<Movie> findByGenresNameContaining(@Param("keyword") String keyword, Pageable pages);

    //PHIM MOI NHAT
    List<Movie> findTop10ByOrderByCreatedAtDesc();

    //PHIM HOT NHAT
    @Query("""
    SELECT m FROM Movie m
    LEFT JOIN CinemaShow s ON s.movie.id = m.id
    LEFT JOIN Booking b ON b.show.id = s.id AND b.status = linh.vn.cinegoticket.enums.BookingStatus.PAID
    GROUP BY m.id
    ORDER BY COUNT(b.id) DESC
    """)
    List<Movie> findTopMoviesByPopularity();

}
