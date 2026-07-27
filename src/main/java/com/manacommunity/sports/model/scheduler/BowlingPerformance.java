package com.manacommunity.sports.model.scheduler;

import com.manacommunity.sports.model.AuctionPlayer;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "bowling_performance")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BowlingPerformance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "innings_id", nullable = false)
    private MatchInnings innings;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private AuctionPlayer player;

    @Builder.Default private Integer bowlingOrder  = 0;
    @Builder.Default private BigDecimal oversBowled = BigDecimal.ZERO;
    @Builder.Default private Integer maidens       = 0;
    @Builder.Default private Integer runsConceded  = 0;
    @Builder.Default private Integer wicketsTaken  = 0;
    @Builder.Default private BigDecimal economyRate = BigDecimal.ZERO;
    @Builder.Default private Integer dotBalls      = 0;
    @Builder.Default private Integer wides         = 0;
    @Builder.Default private Integer noBalls       = 0;

    private LocalDateTime createdAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}
