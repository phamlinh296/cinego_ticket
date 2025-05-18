package linh.vn.cinegoticket.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

import static org.springframework.web.servlet.function.ServerResponse.badRequest;

@RestController
@RequestMapping("/api/movie")
public class MovieController {
    @Autowired
    private MovieService mService;

    @GetMapping("/getall")
    public ResponseEntity<List<MovieResponse>> getMovies(@RequestParam(defaultValue = "0") @Min(0) Integer pageNumber,
                                                         @RequestParam(defaultValue = "100000") @Min(1) Integer pageSize) {
        if (pageSize < 1) {
            return ResponseEntity.badRequest().build(); // Trả về BadRequest mà không có thông báo lỗi chi tiết
        }
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

    //XÓA CACHE
    @DeleteMapping("/clear-cache")
    public ResponseEntity<String> clearCache() {
        mService.evictAllCache(); // Gọi phương thức xóa cache
        return ResponseEntity.ok("All caches have been cleared.");
    }
}
