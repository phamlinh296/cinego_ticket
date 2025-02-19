package linh.vn.cinegoticket.exception;

import jakarta.validation.ConstraintViolation;
import linh.vn.cinegoticket.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.rmi.AccessException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final String MIN_ATTRIBUTE = "min";

    //AppExcep - ex tùy chỉnh
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException exception) {
        ApiResponse<?> apiResponse = new ApiResponse<>();

        ErrorCode errorCode = exception.getErrorCode();

        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage());
        apiResponse.setData(null);

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    //Excep chưa định nghĩa
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        ApiResponse<?> apiResponse = new ApiResponse<>();
        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    //Excep 401: access token hết hạn
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "status", ex.getStatusCode().value(),
                        "error", ex.getStatusCode().toString(),
                        "message", ex.getReason(),
                        "timestamp", new Date()
                ));
    }

    //Excep AccessDenied- không có quyền truy cập - 403 FORBIDDEN.
    @ExceptionHandler(value = AccessException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    //Excep khi validation
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationApi(MethodArgumentNotValidException exception) {
        ApiResponse<?> apiResponse = new ApiResponse<>();
        ErrorCode errorCode;

        Map<String, Object> attributes = null;
        try {
            errorCode = ErrorCode.valueOf(exception.getFieldError().getDefaultMessage());
            // Lấy thuộc tính validation từ lỗi
            var constraintViolation = exception.getBindingResult().getAllErrors().get(0).unwrap(ConstraintViolation.class);
            attributes = constraintViolation.getConstraintDescriptor().getAttributes();
            log.info(attributes.toString());
        } catch (IllegalArgumentException illegalArgumentException) {
            errorCode = ErrorCode.INVALID_KEY;

            apiResponse.setCode(errorCode.getCode());
            apiResponse.setMessage(exception.getMessage());
            return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
        }

        // Tùy chỉnh thông báo lỗi nếu có attributes
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(Objects.nonNull(attributes)
                ? mapAttribute(errorCode.getMessage(), attributes)
                : errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));//get(MIN_ATTRIBUTE)= get("min"); min chính là gtr kbao @DobConstraint(min=18)

        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}

