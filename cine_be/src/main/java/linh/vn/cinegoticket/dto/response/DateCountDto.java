package linh.vn.cinegoticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.sql.Date;

@NoArgsConstructor
@Data
@AllArgsConstructor
public class DateCountDto {
    private LocalDate date; // yyyy-MM-dd
    private Long count;

}
