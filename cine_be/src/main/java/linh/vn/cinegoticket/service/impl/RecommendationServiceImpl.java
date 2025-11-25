package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.entity.Booking;
import linh.vn.cinegoticket.entity.Genre;
import linh.vn.cinegoticket.entity.Movie;
import linh.vn.cinegoticket.repository.BookingRepository;
import linh.vn.cinegoticket.repository.MovieRepository;
import linh.vn.cinegoticket.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final BookingRepository bookingRepo;
    private final MovieRepository movieRepo;

    //  MOVIE VECTOR CACHE   {movieId, {genre, value}} - tránh phải tính lại vector cho cùng một phim nhiều lần.
    private final Map<Long, Map<String, Integer>> movieVectorCache = new HashMap<>();


    // Build vector for movie (cached)
    public Map<String, Integer> buildMovieVector(Movie movie) {
        return movieVectorCache.computeIfAbsent(movie.getId(), id -> {
            Map<String, Integer> vector = new HashMap<>();
            if (movie.getGenres() != null) {
                movie.getGenres().forEach(g ->
                        vector.put(g.getGenre(), 1)
                );
            }
            return vector;
        });
    }
    //computeIfAbsent(key, mappingFunction) nghĩa là:
    //Nếu movieVectorCache chưa có movie.getId(), nó sẽ chạy hàm mappingFunction để tạo vector mới, lưu vào cache, và trả về vector này.
    //Nếu đã có trong cache, trả về ngay vector đã lưu.
    //
    //Trong lambda:
    //Tạo vector mới.
    //Duyệt tất cả genres, mỗi genre gán giá trị 1.
    //
    //Lợi ích:
    //Giảm tính toán lại nhiều lần cho cùng một phim.
    //Tối ưu cho hệ thống gợi ý phim hoặc hệ thống có nhiều request đọc dữ liệu phim.


    // NẾU K CACHE
//    public Map<String, Integer> buildMovieVector(Movie movie) {
//        Map<String, Integer> vector = new HashMap<>();
//        for (Genre g : movie.getGenres()) {
//            vector.put(g.getGenre(), 1); // genre có thì =1
//        }
//        return vector;
//    }
    //Mỗi lần gọi buildMovieVector, luôn tạo một map mới từ đầu.


    // Build vector for user (GENRE COUNT)
    public Map<String, Integer> buildUserVector(String userId) {
        List<Booking> bookings =
                bookingRepo.findPaidBookingsWithMovies(userId);

        Map<String, Integer> vector = new HashMap<>();

        for (Booking booking : bookings) {
            Movie movie = booking.getShow().getMovie();
            if (movie.getGenres() != null) { // tránh NPE
                for (Genre g : movie.getGenres()) {
                    vector.put(g.getGenre(),
                            vector.getOrDefault(g.getGenre(), 0) + 1);
                }
            }
        }

        return vector;
    }


    // Cosine Similarity
    public double cosine(Map<String, Integer> userVector,
                         Map<String, Integer> movieVector) {

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(userVector.keySet());
        allKeys.addAll(movieVector.keySet());

        double dot = 0, userNorm = 0, movieNorm = 0;

        for (String key : allKeys) {
            int u = userVector.getOrDefault(key, 0);
            int m = movieVector.getOrDefault(key, 0);

            dot += u * m;
            userNorm += u * u;
            movieNorm += m * m;
        }

        if (userNorm == 0 || movieNorm == 0) return 0;

        return dot / (Math.sqrt(userNorm) * Math.sqrt(movieNorm));
    }

    public List<Movie> recommend(String userId, int topN) {

        // STEP 1 → Build user vector
        Map<String, Integer> userVector = buildUserVector(userId);

        // STEP 2 → fallback for new user (no history) - CHƯA XEM PHIM NÀO > trả về phim mới nhất.
        if (userVector.isEmpty()) {
            return movieRepo.findTop10ByOrderByCreatedAtDesc();
        }

        // STEP 3 → lấy booking user đã xem : Loại bỏ phim đã xem
        List<Booking> bookings = bookingRepo.findPaidBookingsWithMovies(userId);

        Set<Long> watchedMovieIds = bookings.stream()
                .map(b -> b.getShow().getMovie().getId())
                .collect(Collectors.toSet());

        // STEP 4 → lấy tất cả movie
        List<Movie> movies = movieRepo.findAll();

        // STEP 5 → sort theo similarity
        return movies.stream()
                .filter(m -> !watchedMovieIds.contains(m.getId()))  // bỏ phim đã xem
                .filter(m -> m.getGenres() != null && !m.getGenres().isEmpty())
                .sorted((a, b) -> {     // compare để sắp xếp theo điểm similarity
                    double scoreA = cosine(userVector, buildMovieVector(a));
                    double scoreB = cosine(userVector, buildMovieVector(b));
                    return Double.compare(scoreB, scoreA); // DESC
                })
                .limit(topN)
                .collect(Collectors.toList());
    }


}
