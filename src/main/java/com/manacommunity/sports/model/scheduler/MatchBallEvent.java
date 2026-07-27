package com.manacommunity.sports.model.scheduler;

import com.manacommunity.sports.model.AuctionPlayer;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "match_ball_event")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchBallEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private TournamentMatch match;

    @Builder.Default private Integer inningsNumber  = 1;
    @Builder.Default private Integer overNumber     = 0;
    @Builder.Default private Integer ballNumber     = 1;
    @Builder.Default private Integer deliveryNumber = 1;

    @Builder.Default private Integer runsScored = 0;
    @Builder.Default private Boolean isBoundary = false;
    @Builder.Default private Boolean isSix      = false;

    private String extrasType;
    @Builder.Default private Integer extrasRuns = 0;

    @Builder.Default private Boolean isWicket = false;

    @Enumerated(EnumType.STRING)
    private DismissalType dismissalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dismissed_player_id")
    private AuctionPlayer dismissedPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_id")
    private AuctionPlayer fielder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batsman_id", nullable = false)
    private AuctionPlayer batsman;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "non_striker_id")
    private AuctionPlayer nonStriker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bowler_id", nullable = false)
    private AuctionPlayer bowler;

    @Builder.Default private Integer totalRuns    = 0;
    @Builder.Default private Integer totalWickets = 0;
    @Builder.Default private String  totalOvers   = "0.0";

    private String commentary;
    @Builder.Default private Boolean isUndone = false;

    private Long createdBy;
    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
