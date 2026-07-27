package com.manacommunity.sports.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all mana-community business errors.
 * Carries an HTTP status so the global handler can respond correctly.
 */
public class ManaCommunityException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ManaCommunityException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ManaCommunityException(String message, HttpStatus status, String errorCode, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
