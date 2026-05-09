package linh.vn.cinegoticket.repository;

import linh.vn.cinegoticket.entity.SpamUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpamUserRepository extends JpaRepository<SpamUser, String> {
    Optional<SpamUser> findByUserId(String user_id);

    Boolean existsByUserId(String user_id);
}
