package com.manacommunity.sports.model.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionPlayer;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "batting_performance")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BattingPerformance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "innings_id", nullable = false)
    private MatchInnings innings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private AuctionPlayer player;

    @Builder.Default private Integer battingPosition = 0;
    @Builder.Default private Integer runsScored      = 0;
    @Builder.Default private Integer ballsFaced      = 0;
    @Builder.Default private Integer fours            = 0;
    @Builder.Default private Integer sixes            = 0;
    @Builder.Default private BigDecimal strikeRate   = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private DismissalType dismissalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dismissed_by_id")
    private AuctionPlayer dismissedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fielder_id")
    private AuctionPlayer fielder;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}

