package com.manacommunity.sports.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.manacommunity.sports.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Writes the application's standard {@link ErrorResponse} envelope directly to
 * the servlet response for failures that occur inside the security filter chain
 * (authentication / authorization), which never reach {@code @RestControllerAdvice}.
 *
 * This keeps filter-level 401/403 responses byte-for-byte consistent with the
 * rest of the API — same error code, user-friendly message and correlation id —
 * instead of falling through to Spring's default BasicErrorController body.
 */
@Component
public class SecurityErrorWriter {

    // Self-contained mapper — this app does not expose an injectable ObjectMapper
    // bean. findAndRegisterModules() picks up the JSR-310 module so the
    // ErrorResponse @JsonFormat timestamp renders as an ISO string.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String errorCode, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .correlationId(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID))
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
