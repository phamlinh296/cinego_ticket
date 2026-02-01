package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.entity.Movie;
import linh.vn.cinegoticket.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public ResponseEntity<?> recommend(@RequestParam(required = false) String userId) {
        // Logic: nếu userId truyền lên qua ?userId=... thì dùng, không thì lấy phim mới
        List<Movie> recommended = recommendationService.recommend(userId, 8);
        return ResponseEntity.ok(recommended);
    }
}
