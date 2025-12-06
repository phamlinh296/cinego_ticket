package linh.vn.cinegoticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DateCountDto {
    private String date; // yyyy-MM-dd
    private long count;
}
