package com.kore.king.exception;

// Base exception

import org.springframework.http.HttpStatus;

public abstract class BetKingException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    public BetKingException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}