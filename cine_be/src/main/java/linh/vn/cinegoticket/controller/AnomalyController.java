package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.dto.response.AnomalyStatsResponse;
import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import linh.vn.cinegoticket.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
