package com.manacommunity.sports.model;

import com.manacommunity.sports.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * User-facing notification inbox entry. Each row represents a single
 * notification delivered to a specific user. Supports read/dismiss lifecycle.
 */
@Entity
@Table(name = "notification", indexes = {
    @Index(name = "idx_notif_user_read",    columnList = "user_id, is_read"),
    @Index(name = "idx_notif_user_created", columnList = "user_id, created_at"),
    @Index(name = "idx_notif_ref",          columnList = "reference_type, reference_id"),
    @Index(name = "idx_notif_community",    columnList = "community_id")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private NotificationCategory category = NotificationCategory.GENERAL;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(length = 50)
    private String icon;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 50)
    private ReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(length = 200)
    private String channels;

    @Column(name = "is_read")
    @Builder.Default
    private boolean read = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_dismissed")
    @Builder.Default
    private boolean dismissed = false;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
