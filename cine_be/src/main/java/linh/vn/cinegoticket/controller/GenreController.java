package linh.vn.cinegoticket.controller;

import jakarta.validation.Valid;
import linh.vn.cinegoticket.entity.Genre;
import linh.vn.cinegoticket.repository.GenreReposity;
import linh.vn.cinegoticket.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/genre")
public class GenreController {
    @Autowired
    private GenreService gService;

    @Autowired
    private GenreReposity gReposity;

    //	Lấy danh sách tất cả các thể loại phim.
    @GetMapping("/getall")
    public ResponseEntity<List<Genre>> getGenre() {
        return new ResponseEntity<List<Genre>>(gService.getGenres(), HttpStatus.OK);
    }

    @GetMapping("/byid/{id}")
    public Genre getGenre(@PathVariable(name = "id") @Valid Long id) {
        return gService.getGenre(id);
    }

    @GetMapping("/byname/{genre}")
    public List<Genre> findByGenre(@PathVariable @Valid String genre) {
        return gReposity.findAllByGenre(genre);
    }

    //ADMIN:
    //Thêm một thể loại phim mới.
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Genre> saveGenre(@RequestBody @Valid Genre genre) {
        return ResponseEntity.ok(gService.createGenre(genre));
    }

    //Cập nhật thông tin của một thể loại phim dựa trên ID
    @PutMapping("update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Genre updateGenre(@PathVariable(name = "id") @Valid Long id, @RequestBody @Valid Genre genre) {
        genre.setId(id);
        return gService.updateGenre(genre);
    }

    // @DeleteMapping("/{id}/delete")
    // @PreAuthorize("hasRole('ADMIN')")
    // public ApiResponse deleteGenre (@PathVariable(name="id") @Valid Long id) {
    // return gService.deleteGenre(id);
    // }
}
