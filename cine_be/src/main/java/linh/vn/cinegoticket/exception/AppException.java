package linh.vn.cinegoticket.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private ErrorCode errorCode;
    private String message;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

//    public AppException(String message) {
//        super(message);
//        this.message = message;
//    }

}
