package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.CinemaShow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShowRepository extends JpaRepository<CinemaShow, String> {

}
