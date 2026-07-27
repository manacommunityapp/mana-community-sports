package com.manacommunity.sports.security;

import com.manacommunity.sports.user.security.UserPrincipal;

import com.manacommunity.sports.model.AuditLog;
import com.manacommunity.sports.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Durable, queryable audit trail. Persists each business mutation to the
 * {@code audit_log} table and mirrors a one-line summary to the {@code AUDIT}
 * logger (routed to audit.log).
 *
 * <p>Actor, client IP and correlation id are resolved from the current request /
 * security context. Auditing must never break the business flow: all failures are
 * swallowed and reported to the logger, never propagated.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    private final AuditLogRepository auditLogRepository;

    /** Full record. {@code oldValue}/{@code newValue} should already be safe (no secrets). */
    public void record(AuditAction action, AuditModule module, String entityName, String entityId,
                       String oldValue, String newValue) {
        Long userId = currentUserId();
        String ip = clientIp();
        String correlationId = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
        try {
            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .action(action.name())
                    .module(module.name())
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(ip)
                    .correlationId(correlationId)
                    .build();
            auditLogRepository.save(entry);
            AUDIT.info("action={} module={} entity={} entityId={} userId={} ip={}",
                    action, module, entityName, entityId, userId, ip);
        } catch (Exception ex) {
            // Never let an audit failure break the business transaction.
            AUDIT.error("Failed to persist audit entry action={} module={} entityId={}: {}",
                    action, module, entityId, ex.getMessage());
        }
    }

    /** Convenience overload for create/simple events with no before-state. */
    public void record(AuditAction action, AuditModule module, String entityName, String entityId) {
        record(action, module, entityName, entityId, null, null);
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return null;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
