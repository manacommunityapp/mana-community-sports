package com.manacommunity.sports.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Structured API access log: one INFO line when a request arrives and one when it
 * completes (with status, latency and response size). Correlation id and user id
 * are already on every line via the MDC ({@link CorrelationIdFilter}).
 *
 * <p>Logs <strong>metadata only</strong> — never request/response bodies — so it
 * adds no buffering and is safe for streaming endpoints (SSE/STOMP/downloads).
 * Sensitive query parameters are masked via {@link MaskingUtil}. Noisy infra and
 * polling endpoints are skipped to keep the log signal high.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // just after CorrelationIdFilter so cid is in the MDC
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger("API");
    private static final Logger PERF = LoggerFactory.getLogger("PERFORMANCE");

    /** Latency thresholds for slow-API alerting. */
    private static final long SLOW_WARN_MS = 2_000;
    private static final long SLOW_ERROR_MS = 5_000;

    /** Query-param keys whose values must never be logged in clear. */
    private static final Set<String> SENSITIVE_PARAMS = Set.of(
            "password", "pass", "newpassword", "currentpassword", "otp", "pin", "cvv",
            "token", "accesstoken", "access_token", "refreshtoken", "refresh_token",
            "aadhaar", "aadhar", "secret", "apikey", "api_key"
    );

    /** Endpoints excluded from access logging (infra, docs, streaming, log-polling). */
    private static final Set<String> SKIP_PREFIXES = Set.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars",
            "/ws", "/api/admin/logs", "/api/admin/system-stats", "/api/frontend-logs"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        for (String prefix : SKIP_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        long startNanos = System.nanoTime();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = maskQuery(request.getQueryString());
        String ip = clientIp(request);

        LOG.info("--> {} {}{} ip={}", method, uri, query, ip);
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            LOG.info("<-- {} {} status={} durationMs={} size={}",
                    method, uri, response.getStatus(), durationMs, responseSize(response));

            // Slow-API alerting: WARN over 2s, ERROR over 5s.
            if (durationMs >= SLOW_ERROR_MS) {
                PERF.error("SLOW_API {} {} took {}ms (>{}ms)", method, uri, durationMs, SLOW_ERROR_MS);
            } else if (durationMs >= SLOW_WARN_MS) {
                PERF.warn("SLOW_API {} {} took {}ms (>{}ms)", method, uri, durationMs, SLOW_WARN_MS);
            }
        }
    }

    /** Masks the value of any sensitive query parameter, preserving the rest of the string. */
    private String maskQuery(String queryString) {
        if (queryString == null || queryString.isBlank()) return "";
        StringBuilder sb = new StringBuilder("?");
        String[] pairs = queryString.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq);
                String value = SENSITIVE_PARAMS.contains(key.toLowerCase()) ? MaskingUtil.REDACTED : pair.substring(eq + 1);
                sb.append(key).append('=').append(value);
            } else {
                sb.append(pair);
            }
            if (i < pairs.length - 1) sb.append('&');
        }
        return sb.toString();
    }

    private String responseSize(HttpServletResponse response) {
        String len = response.getHeader("Content-Length");
        return (len == null || len.isBlank()) ? "-" : len;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
