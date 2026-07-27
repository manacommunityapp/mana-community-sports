package com.manacommunity.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Durable audit trail row — one record per sensitive mutation (who did what,
 * to which entity, with before/after values, from where).
 *
 * <p>{@code action} and {@code module} are stored as plain strings (not
 * {@code @Enumerated}) on purpose: a future rename/removal of an enum constant
 * must never break reads of historical rows.</p>
 */
@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id, created_at"),
        @Index(name = "idx_audit_module", columnList = "module, created_at"),
        @Index(name = "idx_audit_action", columnList = "action, created_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Actor — null for system/anonymous actions. */
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false, length = 40)
    private String module;

    @Column(name = "entity_name", length = 60)
    private String entityName;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "old_value", columnDefinition = "text")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "text")
    private String newValue;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Correlation id of the originating request, linking the audit row to the logs. */
    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
