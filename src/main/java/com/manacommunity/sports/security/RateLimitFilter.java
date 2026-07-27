package com.manacommunity.sports.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight, dependency-free per-client fixed-window rate limiter. Protects the
 * API from brute-force and basic denial-of-service by capping how many requests a
 * single client IP may make per minute. Runs before Spring Security so it also
 * shields unauthenticated endpoints (login/register).
 *
 * <p>Configurable via {@code app.security.rate-limit.*}. Tune the limit to your
 * traffic; the default is deliberately generous so normal UI usage is unaffected.</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;
    /** Hard cap on tracked clients to bound memory (the limiter must not itself be a DoS vector). */
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    private final boolean enabled;
    private final int limit;
    private final boolean trustForwardedHeader;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.requests-per-minute:300}") int requestsPerMinute,
            @Value("${app.security.rate-limit.trust-forwarded-header:false}") boolean trustForwardedHeader) {
        this.enabled = enabled;
        this.limit = requestsPerMinute;
        this.trustForwardedHeader = trustForwardedHeader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // Only meter API traffic; let static assets / swagger through untouched.
        if (!enabled || !request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = clientIp(request);
        long now = System.currentTimeMillis();

        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= WINDOW_MS);
        }

        Window window = windows.compute(clientId, (k, current) -> {
            if (current == null || now - current.start >= WINDOW_MS) {
                return new Window(now); // fresh window, count starts at 1
            }
            current.count.incrementAndGet();
            return current;
        });

        if (window.count.get() > limit) {
            long retryAfter = Math.max(1, (WINDOW_MS - (now - window.start)) / 1000);
            writeTooManyRequests(request, response, retryAfter);
            log.warn("Rate limit exceeded for {} on {}", clientId, request.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        if (trustForwardedHeader) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                int comma = forwarded.indexOf(',');
                return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletRequest request, HttpServletResponse response,
                                      long retryAfterSeconds) throws IOException {
        response.setStatus(429); // 429 Too Many Requests
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        String safePath = request.getRequestURI()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "");
        String body = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":429,"
                + "\"error\":\"RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Too many requests. Please slow down and retry shortly.\","
                + "\"path\":\"" + safePath + "\"}";
        response.getWriter().write(body);
    }

    private static final class Window {
        final long start;
        final AtomicInteger count;

        Window(long start) {
            this.start = start;
            this.count = new AtomicInteger(1);
        }
    }
}
