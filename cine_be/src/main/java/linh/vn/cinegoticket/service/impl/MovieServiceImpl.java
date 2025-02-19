package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.MovieResponse;
import linh.vn.cinegoticket.entity.Genre;
import linh.vn.cinegoticket.entity.Movie;
import linh.vn.cinegoticket.exception.AppException;
import linh.vn.cinegoticket.exception.ErrorCode;
import linh.vn.cinegoticket.repository.GenreReposity;
import linh.vn.cinegoticket.repository.MovieRepository;
import linh.vn.cinegoticket.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MovieServiceImpl implements MovieService {
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private InputValidationService inputValidationService;
    @Autowired
    private GenreReposity genreReposity;

    @Override
    public List<MovieResponse> getMovies(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<Movie> page = movieRepository.findAll(pageable);
        List<Movie> movies = page.getContent();

        List<MovieResponse> movieResponses = new ArrayList<>();
        for (Movie movie : movies) {
            movieResponses.add(new MovieResponse(movie));
        }
        return movieResponses;
    }

    @Override
    public MovieResponse getMovie(Long id) {
        Movie movie = movieRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        return new MovieResponse(movie);
    }

    @Override
    public List<MovieResponse> getMatchingName(String title, int pageNumber, int pageSize) {
        String validTitle = inputValidationService.sanitizeInput(title);
        System.out.println("Valid Title: " + validTitle);

        if (inputValidationService.containsSqlInjection(validTitle))
            throw new RuntimeException("Data contains illegal character");

        //phân trang cho kq trả về
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Movie> movies = movieRepository.findByTitleContaining(validTitle, pageable);
        List<MovieResponse> movieResponses = new ArrayList<>();
        for (Movie movie : movies) {
            movieResponses.add(new MovieResponse(movie));
        }
        return movieResponses;
    }

    @Override
    public List<MovieResponse> getMatchingGenre(String genre, int pageNumber, int pageSize) {
        String key = inputValidationService.sanitizeInput(genre);
        System.out.println("Valid Title: " + key);
        if (inputValidationService.containsSqlInjection(key))
            throw new RuntimeException("Data contains illegal character");

        //phân trang cho kq trả về
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        List<Movie> movies = movieRepository.findByGenresNameContaining(key, pageable);
        if (movies.isEmpty())
            return new ArrayList<>();

        List<MovieResponse> movieResponses = new ArrayList<>();
        for (Movie movie : movies) {
            movieResponses.add(new MovieResponse(movie));
        }
        //loại trùng lặp
        Set<MovieResponse> movieResponseSet = new HashSet<>(movieResponses);
        return new ArrayList<>(movieResponseSet);
    }

    //ADMIN
    @Override
    public Movie saveMovie(Movie movie) {
        if (movieRepository.existsByTitle(movie.getTitle()))
            throw new AppException(ErrorCode.EXISTED);
        //chỉ thêm movie vs genre sẵn
        List<Genre> genres = movie.getGenres().stream()
                .map(genre -> genreReposity.findByGenre(genre.getGenre())).collect(Collectors.toList());
        movie.setGenres(genres);
        return movieRepository.save(movie);
    }

    public ApiResponse saveMovieList(List<Movie> movies) {
        int successCount = 0;
        // Lưu tất cả phim vào db mà không kiểm tra trùng lặp
        for (Movie movie : movies) {
            try {
                this.saveMovie(movie);
                successCount++;
            } catch (AppException e) {
                throw e;
            }
        }
        return new ApiResponse<>(String.format("Successfully saved %d movies.", successCount));
    }

    @Override
    public Movie updateMovie(Long id, Movie movie) {
        Movie moviedb = movieRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        moviedb.setId(id);
        if (movie.getTitle() != moviedb.getTitle())
            moviedb.setTitle(movie.getTitle());
        if (movie.getDescription() != moviedb.getDescription())
            moviedb.setDescription(movie.getDescription());

        if (movie.getGenres() != moviedb.getGenres())
            moviedb.setGenres(movie.getGenres());
        if (movie.getImage() != moviedb.getImage())
            moviedb.setImage(movie.getImage());

        return movieRepository.save(moviedb);
    }

    @Override
    public ApiResponse deleteMovie(Long id) {
        movieRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_EXISTED));
        movieRepository.deleteById(id);
        return new ApiResponse("Deleted moive ID " + id);
    }
}
