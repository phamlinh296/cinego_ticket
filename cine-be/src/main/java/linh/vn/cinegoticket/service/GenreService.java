package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.entity.Genre;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface GenreService {
    Genre getGenre(Long id);

    List<Genre> getGenres();

    Genre createGenre(Genre genre);

    Genre updateGenre(Genre genre);

    ApiResponse deleteGenre(Long id);

}

