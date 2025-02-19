package linh.vn.cinegoticket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),//lỗi k xđ = lỗi 500
    EXISTED(1002, "User exited", HttpStatus.BAD_REQUEST),//lỗi lquan input user =bad request
    NOT_EXISTED(1000, "Not found. User does not exist", HttpStatus.NOT_FOUND),//ko tìm thấy = not found =404
    USERNAME_INVALID(1003, "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_KEY(1001, "lỗi ko nằm trong enum errorcode đã kbao", HttpStatus.BAD_REQUEST),//kiểu hằng này k nằm trong enum đã kbao
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),//pas đăng nhập ko khớp - k đăng nhập đc =401
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),//lỗi user không có quyền truy cap
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    ;
    private int code;
    private String message;
    private HttpStatusCode statusCode;

    //cons
    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
