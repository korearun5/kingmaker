package com.kore.king.exception;

import org.springframework.http.HttpStatus;

public class InvalidBetStateException extends BetKingException {
    public InvalidBetStateException(String message) {
        super(message, "INVALID_BET_STATE", HttpStatus.BAD_REQUEST);
    }
}
