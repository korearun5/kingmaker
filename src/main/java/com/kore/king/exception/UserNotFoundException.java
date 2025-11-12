package com.kore.king.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends BetKingException {
    public UserNotFoundException(String username) {
        super("User not found: " + username, "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}
