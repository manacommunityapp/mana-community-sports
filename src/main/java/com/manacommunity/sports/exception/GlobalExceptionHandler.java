package com.manacommunity.sports.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Global exception handler — intercepts every exception thrown by any
 * @RestController and converts it into a consistent ErrorResponse JSON body.
 *
 * Handler priority (top to bottom):
 *   1. ManaCommunityException subtypes — business-specific errors
 *   2. Spring Security exceptions     — authentication / authorisation
 *   3. Spring MVC validation errors   — @Valid / @RequestParam / type mismatch
 *   4. Generic fallback               — any unhandled Throwable → 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 1. Business Exceptions ──────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource: {}", ex.getDiagnosticDetail());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInvite(
            InvalidInviteCodeException ex, HttpServletRequest request) {
        log.warn("Invalid invite code: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegistrationClosedException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationClosed(
            RegistrationClosedException ex, HttpServletRequest request) {
        log.warn("Registration closed: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(EventFullException.class)
    public ResponseEntity<ErrorResponse> handleEventFull(
            EventFullException ex, HttpServletRequest request) {
        log.warn("Event full: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyRegistered(
            AlreadyRegisteredException ex, HttpServletRequest request) {
        log.warn("Already registered: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AgeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleAgeMismatch(
            AgeMismatchException ex, HttpServletRequest request) {
        log.warn("Age mismatch: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InsufficientBudgetException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBudget(
            InsufficientBudgetException ex, HttpServletRequest request) {
        log.warn("Insufficient budget: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidBidAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBid(
            InvalidBidAmountException ex, HttpServletRequest request) {
        log.warn("Invalid bid: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedActionException ex, HttpServletRequest request) {
        log.warn("Unauthorized action: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoPlayersInQueueException.class)
    public ResponseEntity<ErrorResponse> handleNoPlayers(
            NoPlayersInQueueException ex, HttpServletRequest request) {
        log.warn("No players in queue: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(AuctionStateException.class)
    public ResponseEntity<ErrorResponse> handleAuctionState(
            AuctionStateException ex, HttpServletRequest request) {
        log.warn("Auction state error: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidFileUploadException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(
            InvalidFileUploadException ex, HttpServletRequest request) {
        log.warn("Invalid file upload: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(CsvParseException.class)
    public ResponseEntity<ErrorResponse> handleCsvParse(
            CsvParseException ex, HttpServletRequest request) {
        log.warn("CSV parse error at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(
            InvalidInputException ex, HttpServletRequest request) {
        log.warn("Invalid input at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(EncryptionException.class)
    public ResponseEntity<ErrorResponse> handleEncryption(
            EncryptionException ex, HttpServletRequest request) {
        log.error("Encryption failure at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(),
                "An internal error occurred. Please try again later.", request, null);
    }

    /** Catch-all for any other ManaCommunityException subtype. */
    @ExceptionHandler(ManaCommunityException.class)
    public ResponseEntity<ErrorResponse> handleManaCommunity(
            ManaCommunityException ex, HttpServletRequest request) {
        log.error("Business exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    // ─── 1b. Fallback for remaining raw Java exceptions ─────────────────────
    // These catch any IllegalArgumentException / IllegalStateException that was
    // NOT converted to a custom exception (e.g. from third-party code or startup
    // guards). New service code should use typed exceptions above instead.

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                messageOr(ex, "The request contained an invalid value."), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "OPERATION_NOT_ALLOWED",
                messageOr(ex, "This action cannot be performed in the current state."), request, null);
    }

    /** Bare Optional.orElseThrow() / empty stream lookups → 404 (not a 500). */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNoSuchElement(
            NoSuchElementException ex, HttpServletRequest request) {
        log.warn("Missing entity at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "The requested item could not be found.", request, null);
    }

    // ─── 2. Spring Security Exceptions ───────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied to {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action.", request, null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failure at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Authentication required. Please log in and try again.", request, null);
    }

    // ─── 3. Validation Exceptions ────────────────────────────────────────────

    /**
     * Handles @Valid failures on @RequestBody DTOs.
     * Returns per-field validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Validation failed on {} field(s) at {}", fieldErrors.size(), request.getRequestURI());
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed. Check 'fieldErrors' for details.", request, fieldErrors);
    }

    /**
     * Handles @Validated failures on @RequestParam / @PathVariable.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(cv -> ErrorResponse.FieldError.builder()
                        .field(extractField(cv))
                        .message(cv.getMessage())
                        .build())
                .collect(Collectors.toList());

        return build(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION",
                "One or more parameters are invalid.", request, fieldErrors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing.", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        return build(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH",
                "Parameter '" + ex.getName() + "' must be of type " + expected + ".", request, null);
    }

    /** Missing or malformed JSON request body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Request body is missing or malformed.", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.", request, null);
    }

    /** Request body sent with an unsupported Content-Type (e.g. text/plain to a JSON endpoint). */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "The request content type is not supported for this endpoint.", request, null);
    }

    /** Unknown route — return JSON 404 instead of the default error page. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "No endpoint found for " + request.getMethod() + " " + request.getRequestURI() + ".", request, null);
    }

    /** Upload exceeded the configured multipart limit (e.g. CSV import). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUpload(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "The uploaded file is too large.", request, null);
    }

    // ─── 3b. Persistence Exceptions ──────────────────────────────────────────

    /**
     * A referenced row is missing (EmptyResultDataAccessException, e.g. deleteById
     * on an id that no longer exists) or a lazy proxy resolved to nothing
     * (Hibernate EntityNotFoundException). These are 404s, not 500s.
     */
    @ExceptionHandler({EmptyResultDataAccessException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleEntityMissing(
            RuntimeException ex, HttpServletRequest request) {
        log.warn("Entity not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "The requested item could not be found.", request, null);
    }

    /** Concurrent update lost the optimistic-lock race — the client should reload and retry. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic lock conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "This record was updated by someone else. Please reload and try again.", request, null);
    }

    /** DB constraint hit (unique/foreign-key/length). Never echo the raw SQL to the UI. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        String cause = ex.getMostSpecificCause() != null
                ? String.valueOf(ex.getMostSpecificCause().getMessage()) : "";

        // A value-too-long overflow (SQLSTATE 22001) is a bad request, not a
        // data conflict — report it as 400 with an accurate message.
        if (cause.contains("22001") || cause.toLowerCase().contains("value too long")) {
            log.warn("Value too long at {}", request.getRequestURI());
            return build(HttpStatus.BAD_REQUEST, "VALUE_TOO_LONG",
                    "One of the provided values is too long. Please shorten it and try again.", request, null);
        }

        String constraintName = extractConstraintName(ex);
        log.error("Data integrity violation at {} [constraint: {}]", request.getRequestURI(), constraintName);
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "The operation conflicts with existing data — a referenced record is missing, "
                        + "or a unique value is already in use.", request, null);
    }

    private static String extractConstraintName(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
        if (msg == null) return "unknown";
        int idx = msg.indexOf("constraint");
        if (idx >= 0) {
            String sub = msg.substring(idx);
            int end = sub.indexOf('\n');
            return end > 0 ? sub.substring(0, Math.min(end, 120)) : sub.substring(0, Math.min(sub.length(), 120));
        }
        return "unknown";
    }

    // ─── 4. Generic Fallback ─────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later or contact support.", request, null);
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String errorCode, String message,
            HttpServletRequest request, List<ErrorResponse.FieldError> fieldErrors) {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .correlationId(org.slf4j.MDC.get("correlationId"))
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private String extractField(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    /** Uses the exception's own message when present, otherwise a safe default. */
    private String messageOr(Exception ex, String fallback) {
        return ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : fallback;
    }
}
