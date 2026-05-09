//package linh.vn.cinegoticket.job;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisCallback;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AnalyticsBatchJob {
//
//    private final StringRedisTemplate redisTemplate;
//
////    @Scheduled(cron = "0 0 1 * * ?") // 1h sáng mỗi ngày
////    public void aggregateDailyRevenue() {
////
////        String date = LocalDate.now().minusDays(1).toString();
////        log.info("Start batch aggregation for {}", date);
////
////        double totalRevenue = 0;
////        for (int hour = 0; hour < 24; hour++) {
////            String key = "revenue:" + date + ":" + hour;
////            String value = redisTemplate.opsForValue().get(key); //24 round trip, nếu dùng pipeline thì chỉ 1 round trip để lấy tất cả 24 key cùng lúc
////            if (value != null) {
////                totalRevenue += Double.parseDouble(value);
////            }
////        }
////
////        log.info("Total revenue of {} = {}", date, totalRevenue);
////        // giả lập lưu DB
////        // analyticsRepository.save(new DailyRevenue(date, totalRevenue));
////        log.info("Finished batch job for {}", date);
////    }
//
//    @Scheduled(cron = "0 0 1 * * ?")
//    public void aggregateDailyRevenue() {
//        String date = LocalDate.now().minusDays(1).toString();
//        log.info("Start batch aggregation for {}", date);
//
//        // 1. Build tất cả 24 key trước
//        List<String> keys = new ArrayList<>();
//        for (int hour = 0; hour < 24; hour++) {
//            keys.add("revenue:" + date + ":" + hour);
//        }
//
//        // 2. Gửi 24 lệnh GET trong 1 round trip duy nhất
//        List<Object> results = redisTemplate.executePipelined(
//                (RedisCallback<Object>) connection -> {
//                    keys.forEach(k ->
//                            connection.stringCommands().get(k.getBytes())
//                    );
//                    return null; // bắt buộc phải return null
//                }
//        );
//
//        // 3. Tính tổng từ kết quả trả về
//        double totalRevenue = results.stream()
//                .filter(Objects::nonNull)
//                .mapToDouble(v -> Double.parseDouble((String) v))
//                .sum();
//
//        log.info("Total revenue of {} = {}", date, totalRevenue);
//        log.info("Finished batch job for {}", date);
//    }
//}
//
//// "Batch job cần đọc 24 keys nên tôi dùng Redis Pipeline để gom thành 1 round trip, giảm network overhead từ O(n) xuống O(1) về số lần kết nối."
//
////Bỏ luôn. Lý do:
////Batch job này đọc key revenue:date:hour từ Redis — key đó do AnalyticsProcessor (đã xóa) ghi vào. Spark AnalyticsJob ghi vào key spark:revenue:date:hour và đồng thời ghi thẳng vào HBase analytics_daily — HBase đã là long-term storage rồi, không cần batch job đọc Redis nữa.
////Nếu muốn giữ logic pipeline cho đẹp thì đổi key thành spark:revenue: nhưng thực ra không cần vì Spark đã handle.