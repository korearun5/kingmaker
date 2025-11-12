package com.kore.king.exception;

import org.springframework.http.HttpStatus;

public class BetNotFoundException extends BetKingException {
    public BetNotFoundException(Long betId) {
        super("Bet not found with ID: " + betId, "BET_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}