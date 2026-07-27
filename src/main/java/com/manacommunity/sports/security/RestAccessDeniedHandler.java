package com.manacommunity.sports.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles authenticated-but-not-authorized requests raised inside the filter
 * chain. Returns a 403 in the standard {@link
 * com.manacommunity.sports.exception.ErrorResponse} envelope, matching the response
 * that GlobalExceptionHandler#handleAccessDenied produces for method-security
 * (@PreAuthorize) denials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("Access denied to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
        errorWriter.write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to perform this action.");
    }
}
