package linh.vn.cinegoticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)//cái nào null thì bỏ qua ko in ra
public class ApiResponse<T> {
    private int code = 1000;
    private String message;
    private T data;
    private String status;

    public ApiResponse(String message) {
        this.message = message;
    }

    //thêm
    public ApiResponse(String message, HttpStatus httpStatus) {
        this.message = message;
        this.status = httpStatus.name();
    }

    public ApiResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

    // STATIC FACTORY METHODS

    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .code(1000)
                .message(message)
                .status("SUCCESS")
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(1000)
                .message(message)
                .data(data)
                .status("SUCCESS")
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(4000)
                .message(message)
                .status("ERROR")
                .build();
    }
}
