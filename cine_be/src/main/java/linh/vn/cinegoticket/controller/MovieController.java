package linh.vn.cinegoticket.controller;

import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.MovieResponse;
import linh.vn.cinegoticket.entity.Movie;
import linh.vn.cinegoticket.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movie")
public class MovieController {
    @Autowired
    private MovieService mService;

    @GetMapping("/getall")
    public ResponseEntity<List<MovieResponse>> getMovies(@RequestParam(defaultValue = "0") Integer pageNumber,
                                                         @RequestParam(defaultValue = "100000") @Valid Integer pageSize) {
        return new ResponseEntity<List<MovieResponse>>(mService.getMovies(pageNumber, pageSize), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovie(@PathVariable(name = "id") @Valid Long id) {
        return mService.getMovie(id);
    }

    @GetMapping("/searchbytitle")
    public ResponseEntity<List<MovieResponse>> getMovieName(@RequestParam @Valid String key,
                                                            @RequestParam(defaultValue = "0") @Valid Integer pageNumber,
                                                            @RequestParam(defaultValue = "50") @Valid Integer pageSize) {
        return new ResponseEntity<List<MovieResponse>>(mService.getMatchingName(key, pageNumber, pageSize),
                HttpStatus.OK);
    }


    @GetMapping("/searchbygenre")
    public ResponseEntity<?> getMovieGenre(@RequestParam @Valid String key,
                                           @RequestParam(defaultValue = "0") @Valid Integer pageNumber,
                                           @RequestParam(defaultValue = "50") @Valid Integer pageSize) {
        return new ResponseEntity<>(mService.getMatchingGenre(key, pageNumber, pageSize), HttpStatus.OK);
    }

    // FOR ADMIN ROLE:

    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public Movie saveMovie(@RequestBody @Valid Movie movie) {
        return mService.saveMovie(movie);
    }

    @PostMapping("/addlist")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> saveMovieList(@RequestBody @Valid List<Movie> movies) {
        return ResponseEntity.ok().body(mService.saveMovieList(movies));
    }

    @PutMapping("/{id}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Movie> updateMovie(@PathVariable(name = "id") @Valid Long id,
                                             @RequestBody @Valid Movie movie) {
        return new ResponseEntity<Movie>(mService.updateMovie(id, movie), HttpStatus.OK);
    }

    @DeleteMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteMovie(@PathVariable(name = "id") @Valid Long id) {
        return ResponseEntity.ok().body(mService.deleteMovie(id));
    }
}
