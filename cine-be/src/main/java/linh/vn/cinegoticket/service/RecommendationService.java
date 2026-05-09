package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.entity.Movie;

import java.util.List;

public interface RecommendationService {
    List<Movie> recommend(String userId, int topN);
}
