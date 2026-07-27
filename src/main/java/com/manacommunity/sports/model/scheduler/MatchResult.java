package com.manacommunity.sports.model.scheduler;

import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.AuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "match_result")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchResult {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private TournamentMatch match;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false)
    @Builder.Default
    private ResultType resultType = ResultType.WIN;

    private String winMargin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "toss_winner_team_id")
    private AuctionTeam tossWinner;

    private String tossDecision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "man_of_match_player_id")
    private AuctionPlayer manOfMatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_batter_player_id")
    private AuctionPlayer bestBatter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "best_bowler_player_id")
    private AuctionPlayer bestBowler;

    @Column(columnDefinition = "TEXT")
    private String matchSummary;

    private String umpires;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
