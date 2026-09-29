package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_notification_scheduler")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsNotificationScheduler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private SportsEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private Tournament tournament;

    @Column(name = "trigger_key", length = 50)
    private String triggerKey; // '7d', '1d', etc.

    @Column(nullable = false, length = 100)
    private String label; // '7 Days Before'

    @Column(name = "offset_minutes", nullable = false)
    private Integer offsetMinutes; // e.g., -10080

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false, length = 500)
    private String recipients; // Comma-separated or JSON list of target audience matrices

    @Column(nullable = false, length = 200)
    private String channels; // Comma-separated override channels e.g., "push,email"

    @Column(nullable = false, length = 20)
    private String priority = "NORMAL";

    @Column(name = "is_custom", nullable = false)
    private Boolean isCustom = false;

    @Column(nullable = false)
    private Boolean sent = false;

    @Column(name = "notify_at")
    private LocalDateTime notifyAt; // Calculated runtime target timestamp

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

