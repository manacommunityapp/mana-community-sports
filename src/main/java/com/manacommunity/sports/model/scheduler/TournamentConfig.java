package com.manacommunity.sports.model.scheduler;

import com.manacommunity.sports.model.Community;

import com.manacommunity.sports.user.model.AppUser;

import com.manacommunity.sports.model.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity @Table(name = "tournament_config")
@Data @Builder(toBuilder = true) @NoArgsConstructor @AllArgsConstructor
public class TournamentConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String tournamentName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id")
    private SportsMeta sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SportsEvent event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentType tournamentType;

    // ── Team setup ────────────────────────────────────────────────
    @Column(nullable = false)
    private Integer totalTeams;

    // ── Group Stage config ────────────────────────────────────────
    private Integer numberOfGroups;        // e.g. 2 (Group A, Group B)
    private Integer teamsPerGroup;         // auto-calculated
    private Integer teamsAdvancingPerGroup;// top N from each group go to knockout

    // ── Knockout config ───────────────────────────────────────────
    private Boolean thirdPlaceMatch;       // play 3rd place match?
    private Boolean hasSeeding;            // seed teams 1..N before draw

    // ── Swiss config ──────────────────────────────────────────────
    private Integer swissRounds;           // usually ceil(log2(teams))

    // ── Schedule timings ──────────────────────────────────────────
    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer matchDurationMinutes;
    private Integer breakBetweenMatchesMinutes;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    // ── Points system (for round robin / group stage) ─────────────
    private Integer pointsForWin;          // default 2
    private Integer pointsForDraw;         // default 1
    private Integer pointsForLoss;         // default 0

    // ── Status ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;       // DRAFT | ACTIVE | LIVE | COMPLETED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL)
    private List<TournamentGroup> groups;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL)
    private List<TournamentMatch> matches;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum TournamentStatus { DRAFT, ACTIVE, LIVE, COMPLETED, CANCELLED }
}
