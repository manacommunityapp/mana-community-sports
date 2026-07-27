package com.manacommunity.sports.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Lightweight security audit trail. Emits a structured line to a dedicated SLF4J
 * logger ({@code SECURITY_AUDIT}) for auth-sensitive events — login, failed login,
 * lockout, logout, registration, token refresh.
 *
 * <p>Stateless by design (no audit table, per the chosen no-new-infra approach):
 * route the {@code SECURITY_AUDIT} logger to its own file/appender in logback if a
 * durable trail is needed. Client IP and User-Agent are resolved from the current
 * request when one is bound to the thread.</p>
 */
@Service
public class AuditLogService {

    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    public enum Action {
        LOGIN_SUCCESS, LOGIN_FAILED, LOGIN_LOCKED, ACCOUNT_LOCKED,
        LOGOUT, REGISTER, TOKEN_REFRESH, TOKEN_REFRESH_FAILED,
        MULTIPLE_DEVICE_LOGIN, PASSWORD_CHANGED, SESSION_EXPIRED, ACCESS_DENIED, TOKEN_EXPIRED
    }

    /** Record an event tied to a known user. */
    public void record(Action action, Long userId, String email) {
        AUDIT.info("action={} userId={} email={} ip={} userAgent=\"{}\"",
                action, userId, mask(email), clientIp(), userAgent());
    }

    /** Record an event where the user may be unknown (e.g. failed login for a bad email). */
    public void record(Action action, String email) {
        record(action, null, email);
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "-";
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String userAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "-";
        String ua = req.getHeader("User-Agent");
        return ua == null ? "-" : ua;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    /** Partially mask the email so the audit log doesn't store full addresses in clear. */
    private String mask(String email) {
        return MaskingUtil.maskEmail(email);
    }
}
