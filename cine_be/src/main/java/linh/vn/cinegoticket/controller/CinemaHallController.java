package linh.vn.cinegoticket.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import linh.vn.cinegoticket.dto.request.CinemaHallRequest;
import linh.vn.cinegoticket.dto.request.SeatEditRequest;
import linh.vn.cinegoticket.entity.CinemaHall;
import linh.vn.cinegoticket.service.CinemaHallService;
import linh.vn.cinegoticket.service.CinemaSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hall")
public class CinemaHallController {

    @Autowired
    private CinemaHallService cinemaHallService;

    @Autowired
    private CinemaSeatService cinemaSeatService;

    //USER

    @GetMapping("/getall")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok().body(cinemaHallService.getAllHalls());
    }


    //ADMIN
    @GetMapping("/{hall_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getbyID(@Valid @PathVariable(value = "hall_id") String hall_id) {
        return ResponseEntity.ok().body(cinemaHallService.getHallById(hall_id));
    }

    @PutMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getall(@Valid @RequestBody CinemaHallRequest cReq) {
        CinemaHall hall = new CinemaHall(cReq);
        return ResponseEntity.ok().body(cinemaHallService.newHall(hall));
    }

    @DeleteMapping("/{hall_id}/delete")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> test1(@PathVariable(value = "hall_id") String hall_id) {
        return ResponseEntity.ok().body(cinemaHallService.removeHall(hall_id));
    }

    @PutMapping("/{hall_id}/edit")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> edit(@PathVariable(value = "hall_id") String hall_id,
                                  @RequestBody CinemaHallRequest cReq) {
        return ResponseEntity.ok().body(cinemaHallService.editHall(hall_id, cReq));
    }

    //seat
    @GetMapping("/{hall_id}/seat/getall")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> geAllSeatstbyHallID(@PathVariable(value = "hall_id") String hall_id) {
        return ResponseEntity.ok().body(cinemaSeatService.getAllSeatsFromHall(hall_id));
    }

    @PutMapping("/{hall_id}/seat/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getall(@PathVariable(value = "hall_id") String hall_id,
                                    @RequestBody SeatEditRequest cReq) {
        return ResponseEntity.ok().body(cinemaSeatService.Edit(hall_id, cReq));
    }
}
