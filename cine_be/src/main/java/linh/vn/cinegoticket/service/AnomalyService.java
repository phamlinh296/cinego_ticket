package linh.vn.cinegoticket.service;

import linh.vn.cinegoticket.dto.response.AnomalyStatsResponse;
import linh.vn.cinegoticket.entity.AnomalyLog;

import java.util.List;

public interface AnomalyService {
    AnomalyStatsResponse getStatsLast7Days();

    List<AnomalyLog> listAll();
}
