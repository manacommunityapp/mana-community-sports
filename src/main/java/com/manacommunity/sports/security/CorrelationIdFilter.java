package com.manacommunity.sports.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes a per-request correlation ID and makes it available to every log
 * line via SLF4J's MDC, so a single request can be traced end-to-end across
 * application, security, audit and exception logs.
 *
 * <ul>
 *   <li>Reads {@code X-Correlation-Id} from the incoming request, or generates one.</li>
 *   <li>Puts {@code correlationId} (and {@code userId}, once authenticated) into the MDC.</li>
 *   <li>Echoes the ID back on the response so the client/frontend can surface it.</li>
 *   <li>Always clears the MDC in {@code finally} to avoid leaking across pooled threads.</li>
 * </ul>
 *
 * Ordered before security so even auth failures are correlated. The MDC keys are
 * rendered by the {@code logging.pattern.*} config in application.yaml.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        try {
            // userId is added to the MDC by JwtAuthenticationFilter once the request
            // is authenticated; this filter owns the lifecycle and clears both keys.
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        if (incoming != null && !incoming.isBlank() && incoming.length() <= 64) {
            // Strip anything that isn't safe for a log token to prevent log forging.
            return incoming.replaceAll("[^a-zA-Z0-9\\-]", "");
        }
        return UUID.randomUUID().toString();
    }
}
