package com.manacommunity.sports.model.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.sports.model.Court;
import com.manacommunity.sports.model.Venue;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.LocalDateTime;

@Entity @Table(name = "tournament_match")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TournamentMatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tournament_match_config"))
    @OnDelete(action = OnDeleteAction.CASCADE)   // delete matches when their config is deleted
    private TournamentConfig config;

    // ── Bracket / group context ───────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TournamentGroup group;   // null for knockout rounds

    @Enumerated(EnumType.STRING)
    private MatchRound round;

    private Integer roundNumber;     // e.g. 1 for QF, 2 for SF, 3 for F
    private Integer matchNumber;     // match 1, 2, 3 in the round
    private Integer bracketSlot;     // position in bracket visualization

    // ── Teams ─────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_a_id")
    private AuctionTeam teamA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_b_id")
    private AuctionTeam teamB;

    // For bracket: which match does the winner come from?
    private Long winnerFeedFromMatchA;   // match id
    private Long winnerFeedFromMatchB;   // match id

    // Where does the winner/loser of THIS match go?
    private Long winnerAdvancesToMatchId;
    private Long loserSentToMatchId;     // for double elimination

    // ── Schedule ──────────────────────────────────────────────────
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    private Integer durationMinutes;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "court_id")
    private Court court;

    // ── Result ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private String  scoreTeamA;    // flexible: "45 runs", "21-18,21-15"
    private String  scoreTeamB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private AuctionTeam winner;

    @Enumerated(EnumType.STRING)
    private ResultType resultType;

    private String  winMargin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_team_id")
    private AuctionTeam tossWinner;

    private String  tossDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "man_of_match_id")
    private AuctionPlayer manOfMatch;

    @Column(columnDefinition = "TEXT")
    private String  matchSummary;

    private String  umpires;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String  matchNotes;

    // ── Swiss round ───────────────────────────────────────────────
    private Integer swissRoundNumber;

    // ── Self-match reminder bookkeeping ───────────────────────────
    /** True once the "your match starts soon" reminder has been emailed (prevents duplicates). */
    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private boolean reminderSent = false;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

