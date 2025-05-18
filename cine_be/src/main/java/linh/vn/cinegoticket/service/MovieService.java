package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.MovieResponse;
import linh.vn.cinegoticket.entity.Movie;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface MovieService {

    List<MovieResponse> getMovies(int pageNumber, int pageSize);

    MovieResponse getMovie(Long id);

    List<MovieResponse> getMatchingName(String title, int pageNumber, int pageSize);

    List<MovieResponse> getMatchingGenre(String genre, int pageNumber, int pageSize);

    //ADMIN
    Movie saveMovie(Movie movie);

    ApiResponse saveMovieList(List<Movie> movies);


    Movie updateMovie(Long id, Movie movie);

    ApiResponse deleteMovie(Long id);

    //xóa cache
    void evictAllCache();

    void evictAllMoviesCache();
}
