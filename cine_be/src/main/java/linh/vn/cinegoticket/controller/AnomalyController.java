package linh.vn.cinegoticket.controller;

import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {

    @Autowired
    private AnomalyLogRepository anomalyLogRepository;

    //admin xem danh sách anomaly logs
    @GetMapping
    public List<AnomalyLog> getAll() {
        return anomalyLogRepository.findAll();
    }
}
