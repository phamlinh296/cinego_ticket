package linh.vn.cinegoticket.controller;

import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.ShowRequest;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import linh.vn.cinegoticket.dto.response.ShowInfoResponse;
import linh.vn.cinegoticket.service.CinemaShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/show")
public class CinemaShowController {

    @Autowired
    private CinemaShowService cinemaShowService;

    // @Autowired
    // private CinemaShowRepository cinemaShowRepository;

    @GetMapping("/{show_id}")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getShowByID(@Valid @PathVariable(value = "show_id") String show_id) {
        return ResponseEntity.ok().body(cinemaShowService.getShowInfo(show_id));
    }

    @GetMapping("/{show_id}/seats")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAllseats(@Valid @PathVariable(value = "show_id") String show_id) {
        return ResponseEntity.ok().body(cinemaShowService.getAllShowSeats(show_id));
    }

    @GetMapping("/frommovie")
//    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getShowByMovieID(@Valid @RequestParam(value = "id") Long movie_id) {
        return ResponseEntity.ok().body(cinemaShowService.getAllShowByMovieID(movie_id));
    }

    @GetMapping("/fromhall")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getShowByHallID(@Valid @RequestParam(value = "id") String hall_id) {
        return ResponseEntity.ok().body(cinemaShowService.getAllShowByHallID(hall_id));
    }

    @GetMapping("/getall")
//    @PreAuthorize("hasRole('ADMIN')")
    public List<ShowInfoResponse> getAllShows() {
        return cinemaShowService.getAllShows();
    }

    @PostMapping("/add")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addShow(@RequestBody @Valid ShowRequest showRequest) {
        return new ResponseEntity<ApiResponse>(cinemaShowService.addShow(showRequest), HttpStatus.OK);
    }

    @PostMapping("/addlist")
//    @PreAuthorize("hasRole('ADMIN')")
    public List<ApiResponse> addShow(@RequestBody @Valid List<ShowRequest> showRequest) {
        return cinemaShowService.addListShows(showRequest);
    }

    @PutMapping("/{id}/update")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateShow(@PathVariable(name = "id") @Valid String id,
                                                  @RequestBody @Valid ShowRequest showReq) {
        return ResponseEntity.ok().body(cinemaShowService.updateShow(id, showReq));
    }

    @DeleteMapping("/{show_id}/delete")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteShowByID(@PathVariable(name = "show_id") @Valid String show_id) {
        return ResponseEntity.ok().body(cinemaShowService.deleteShow(show_id));
    }

    @DeleteMapping("/deletebyhallandmovie")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteShowByHallIDMovieID(@RequestBody @Valid ShowRequest showReq) {
        return ResponseEntity.ok().body(cinemaShowService.deleteShowByHallIDMovieID(showReq));
    }

}
