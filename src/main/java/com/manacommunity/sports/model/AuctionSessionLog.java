package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "auction_session_log")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class AuctionSessionLog {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private AuctionConfig config;

    @Column(nullable = false)
    private String action; // e.g. PLAYER_SOLD, PLAYER_PASSED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private AuctionPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private AuctionTeam team;

    private Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private AppUser performedBy;

    private String notes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime loggedAt;

    @PrePersist void onCreate() { loggedAt = LocalDateTime.now(); }
}


