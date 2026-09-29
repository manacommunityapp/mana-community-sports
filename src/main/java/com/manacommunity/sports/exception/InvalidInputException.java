package com.manacommunity.sports.exception;

import com.manacommunity.common.exception.ManaCommunityException;
import com.manacommunity.common.enums.*;
import org.springframework.http.HttpStatus;

/** Thrown when a service-layer input validation check fails. */
public class InvalidInputException extends ManaCommunityException {

    public InvalidInputException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_INPUT");
    }
}


