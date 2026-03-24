package com.chronos.chronos.exception;


import org.springframework.http.HttpStatusCode;

public class ApiException extends RuntimeException {
    private final HttpStatusCode status;
    private final String errorCode;

    public ApiException(HttpStatusCode status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}