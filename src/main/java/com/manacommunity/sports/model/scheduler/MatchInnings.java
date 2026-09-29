package com.manacommunity.sports.model.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionTeam;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "match_innings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchInnings {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id", nullable = false)
    private MatchResult matchResult;

    @Column(nullable = false)
    @Builder.Default
    private Integer inningsNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batting_team_id", nullable = false)
    private AuctionTeam battingTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bowling_team_id", nullable = false)
    private AuctionTeam bowlingTeam;

    @Builder.Default private Integer totalRuns    = 0;
    @Builder.Default private Integer totalWickets = 0;
    @Builder.Default private BigDecimal totalOvers = BigDecimal.ZERO;
    @Builder.Default private Integer extras       = 0;

    @Column(columnDefinition = "JSON")
    private String extrasDetail;

    private Integer target;

    @Builder.Default
    private BigDecimal runRate = BigDecimal.ZERO;

    @Column(columnDefinition = "JSON")
    private String fallOfWickets;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

