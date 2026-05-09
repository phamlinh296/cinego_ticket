package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.CinemaHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CinemaHallRepository extends JpaRepository<CinemaHall, String> {
    Optional<CinemaHall> findByName(String name);

    boolean existsByName(String name);
}
