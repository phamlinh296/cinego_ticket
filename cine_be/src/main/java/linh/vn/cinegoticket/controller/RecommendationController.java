package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.entity.Movie;
import linh.vn.cinegoticket.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/{userId}")
    public ResponseEntity<?> recommend(@PathVariable String userId) {
        List<Movie> recommended = recommendationService.recommend(userId, 6);
        return ResponseEntity.ok(recommended);
    }
}
