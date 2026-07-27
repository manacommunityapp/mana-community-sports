package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a user attempts an action they are not authorized to perform. */
public class UnauthorizedActionException extends ManaCommunityException {

    public UnauthorizedActionException(String action) {
        super("Unauthorized: " + action, HttpStatus.FORBIDDEN, "UNAUTHORIZED_ACTION");
    }
}
