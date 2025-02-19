package linh.vn.cinegoticket.repository;

import jakarta.validation.constraints.NotBlank;
import linh.vn.cinegoticket.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GenreReposity extends JpaRepository<Genre, Long> {

    boolean existsByGenre(@NotBlank String genre);

    Genre findByGenre(String genre);

    List<Genre> findAllByGenre(String genre);

    List<Genre> findAllByGenreIn(Collection<String> genres);

    List<Genre> findByGenreContaining(String genre);

}
