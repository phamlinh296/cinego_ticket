package linh.vn.cinegoticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AnomalyStatsResponse {
    private List<DateCountDto> byDate;
    private Map<String, Long> byType;
}
