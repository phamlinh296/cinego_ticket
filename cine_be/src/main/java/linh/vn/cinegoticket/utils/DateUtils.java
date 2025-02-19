package linh.vn.cinegoticket.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static LocalDateTime convertStringDateToDate(String date, String format) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            LocalDateTime newDate = LocalDateTime.parse(date, formatter);
            return newDate;
        } catch (Exception e) {
            return null;
        }
    }

}
