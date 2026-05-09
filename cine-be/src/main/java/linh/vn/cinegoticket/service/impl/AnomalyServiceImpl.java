package linh.vn.cinegoticket.service.impl;

import linh.vn.cinegoticket.dto.response.AnomalyStatsResponse;
import linh.vn.cinegoticket.dto.response.DateCountDto;
import linh.vn.cinegoticket.entity.AnomalyLog;
import linh.vn.cinegoticket.repository.AnomalyLogRepository;
import linh.vn.cinegoticket.service.AnomalyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnomalyServiceImpl implements AnomalyService {

    private final AnomalyLogRepository anomalyLogRepository;

//    @Override
//    public AnomalyStatsResponse getStatsLast7Days() {
//        LocalDate today = LocalDate.now();
//        LocalDate from = today.minusDays(6); // 7 days
//
//        LocalDateTime fromDateTime = from.atStartOfDay(ZoneId.systemDefault()).toLocalDateTime();
//
//        List<DateCountDto> byDate = anomalyLogRepository.countByDate(fromDateTime);
//
//        List<Object[]> byTypeRaw = anomalyLogRepository.countByType();
//        Map<String, Long> byType = byTypeRaw.stream()
//                .collect(Collectors.toMap(o -> o[0].toString(), o -> ((Number) o[1]).longValue()));
//
//        return new AnomalyStatsResponse(byDate, byType);
//    }

    @Override
    public List<AnomalyLog> listAll() {
        return anomalyLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")); //Sắp xếp mới nhất trước
    }
}
