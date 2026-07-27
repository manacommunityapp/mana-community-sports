package com.manacommunity.sports.model;

import com.manacommunity.sports.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity 
@Table(name = "auction_config")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class AuctionConfig {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sport_id", nullable = false)
    private SportsMeta sport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private SportsEvent event;

    @Column(nullable = false)
    private String seasonName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionFormat auctionFormat;

    @Column(nullable = false)
    private Integer totalTeams;

    @Column(nullable = false)
    private Integer totalPlayers;

    @Column(nullable = false)
    private Long budgetPerTeam;

    // ── Bidding Rules (all dynamically configurable) ──
    @Column(nullable = false)
    private Integer basePrice;

    @Column(nullable = false)
    private Integer bidIncrementDefault;       // e.g. ₹1,000

    @Column(nullable = false)
    private Long bidIncrementThreshold;        // e.g. ₹10,000

    @Column(nullable = false)
    private Integer bidIncrementAbove;         // e.g. ₹5,000 above threshold

    @Column(nullable = false)
    private Integer bidTimerSeconds;

    @Column(nullable = false)
    private Boolean rtmEnabled;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnsoldRule unsoldRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL)
    private List<AuctionTeam> teams;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<AuctionPlayer> players;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<AuctionDisputeCommittee> committeeMembers;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist  void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate   void onUpdate() { updatedAt = LocalDateTime.now(); }

    /**
     * Core business rule: calculate the next bid increment based on current bid.
     * Rule: below threshold → bidIncrementDefault
     *       at or above threshold → bidIncrementAbove
     */
    public int calculateNextIncrement(long currentBid) {
        return currentBid >= bidIncrementThreshold
            ? bidIncrementAbove
            : bidIncrementDefault;
    }

    public long calculateNextBid(long currentBid) {
        return currentBid + calculateNextIncrement(currentBid);
    }

    public enum AuctionFormat  { OPEN_AUCTION, SILENT_AUCTION, DRAFT_FORMAT }

    public enum AuctionStatus {
        DRAFT, ACTIVE, LIVE, COMPLETED, CANCELLED;

        private static final java.util.Map<AuctionStatus, java.util.Set<AuctionStatus>> ALLOWED =
                java.util.Map.of(
                        DRAFT,     java.util.Set.of(ACTIVE, CANCELLED),
                        ACTIVE,    java.util.Set.of(LIVE, CANCELLED),
                        LIVE,      java.util.Set.of(COMPLETED, CANCELLED),
                        COMPLETED, java.util.Set.of(),
                        CANCELLED, java.util.Set.of()
                );

        public boolean canTransitionTo(AuctionStatus target) {
            return ALLOWED.getOrDefault(this, java.util.Set.of()).contains(target);
        }
    }

    public enum UnsoldRule     { ROTATION_AUCTION, RESERVE_POOL, RE_AUCTION_BASE }
}
