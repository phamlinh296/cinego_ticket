package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/anomalies")
@CrossOrigin(origins = "*") // Thêm dòng này để cho phép FE gọi vào
public class AnomalyController {

    private final AnomalyService anomalyService;

    //Lấy danh sách all log giao dịch bất thường
//    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<AnomalyLog> getAll() {
        return anomalyService.listAll();
    }

    // dashboard - thống kê giao dịch bất thường
//    @GetMapping("/stats")
//    public AnomalyStatsResponse getStats() {
//        return anomalyService.getStatsLast7Days();
//    }
}

//AnomalyConsumer → gọi AnomalyDetectorService
//  → detect fraud NGAY LẬP TỨC khi event đến (<50ms)
//  → Spark KHÔNG thể thay thế vì Spark có latency 30s–1m do window-based processing, không phù hợp để phát hiện fraud real-time.