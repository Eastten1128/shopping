package com.jdr1128.shopping.global.exception;


import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {

    private String code;
    private String message;
    private List<ValidationError> error;
    private LocalDateTime timestamp;

    public ErrorResponse(
            String code,
            String message,
            List<ValidationError> error
    ) {
        this.code = code;
        this.message = message;
        this.error = error;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(ErrorCode errorCode){
        return new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage(),
                List.of()
        );
    }

    //Overloading
    public static ErrorResponse of(
            ErrorCode errorCode,
            List<ValidationError> errors
    ) {
        return new ErrorResponse(
                errorCode.name(),
                errorCode.getMessage(),
                errors
        );
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public List<ValidationError> getError() {
        return error;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public static class ValidationError {

        private String field;
        private String message;

        private ValidationError(String field, String message){
            this.field = field;
            this.message = message;
        }

        public static ValidationError of(String field, String message){
            return new ValidationError(field, message);
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
