package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A custom milestone attached to a {@link Tournament}, shown in the
 * "Tournament Timeline" section of the announcement email. When no custom
 * entries exist the service falls back to milestones derived from the
 * tournament's own registration/event dates.
 */
@Entity
@Table(name = "tournament_timeline_entry")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentTimelineEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnoreProperties({"sportsEvents", "sponsors", "contacts"})
    private Tournament tournament;

    /** Concrete date of the milestone; formatted for display when present. */
    @Column(name = "entry_date")
    private LocalDate entryDate;

    /** Optional display label used when there is no concrete date (e.g. "Opening Day"). */
    @Column(name = "date_label", length = 60)
    private String dateLabel;

    @Column(length = 150, nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

