package com.kore.king.exception;

// Business exceptions

import org.springframework.http.HttpStatus;

public class InsufficientPointsException extends BetKingException {
    public InsufficientPointsException(int available, int required) {
        super("Insufficient points. Available: " + available + ", Required: " + required,
              "INSUFFICIENT_POINTS", HttpStatus.BAD_REQUEST);
    }
}