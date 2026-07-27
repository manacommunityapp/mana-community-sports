package com.manacommunity.sports.model;

import com.manacommunity.sports.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "auction_bid")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class AuctionBid {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private AuctionConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private AuctionPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private AuctionTeam team;

    @Column(nullable = false)
    private Long bidAmount;

    @Column(nullable = false)
    private Integer incrementUsed;

    private Boolean isRtm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_by_user_id")
    private AppUser bidByUser;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bidAt;

    @PrePersist void onCreate() { bidAt = LocalDateTime.now(); }
}
