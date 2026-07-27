package com.manacommunity.sports.model.scheduler;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Immutable audit log of every schedule generation, save, or delete operation.
 * One row per operation — append-only, never updated or deleted.
 */
@Entity
@Table(name = "schedule_generation_log", indexes = {
    @Index(name = "idx_sgl_config",    columnList = "config_id"),
    @Index(name = "idx_sgl_event",     columnList = "event_id"),
    @Index(name = "idx_sgl_community", columnList = "community_id"),
    @Index(name = "idx_sgl_created",   columnList = "created_at")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScheduleGenerationLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_id")
    private Long configId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "generated_by")
    private Long generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private GenerationAction action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GenerationStatus status;

    @Column(name = "tournament_type", length = 50)
    private String tournamentType;

    @Column(name = "total_teams")
    private Integer totalTeams;

    @Column(name = "total_matches")
    private Integer totalMatches;

    @Column(name = "total_groups")
    private Integer totalGroups;

    @Column(name = "venues_used", length = 500)
    private String venuesUsed;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "request_snapshot", columnDefinition = "TEXT")
    private String requestSnapshot;

    @Column(name = "response_snapshot", columnDefinition = "TEXT")
    private String responseSnapshot;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
