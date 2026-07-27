package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an uploaded file fails validation (empty, too large, wrong type). */
public class InvalidFileUploadException extends ManaCommunityException {

    public InvalidFileUploadException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_FILE");
    }
}
