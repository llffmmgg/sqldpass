package com.sqldpass.service.common;

public class SqldpassException extends RuntimeException {

    private final ErrorCode errorCode;

    public SqldpassException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public SqldpassException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SqldpassException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
