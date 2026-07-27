package com.manacommunity.sports.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response body returned for every exception.
 *
 * Example JSON:
 * {
 *   "timestamp": "2026-04-24T11:45:00",
 *   "status": 409,
 *   "error": "DUPLICATE_RESOURCE",
 *   "message": "A User with that email already exists.",
 *   "path": "/api/auth/register",
 *   "fieldErrors": null
 * }
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private final int status;

    private final String error;     // machine-readable error code

    private final String message;   // human-readable description

    private final String path;      // request URI that triggered the error

    /** Request correlation id — lets a user quote it to support to find the exact log trail. */
    private final String correlationId;

    /** Per-field validation errors (populated only for @Valid failures). */
    private final List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
