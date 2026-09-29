package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity 
@Table(name = "auction_team")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class AuctionTeam {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private AuctionConfig config;

    @Column(nullable = false)
    private String teamName;

    private String ownerName;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private AppUser ownerUser;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "captain_user_id")
    private AppUser captainUser;

    @Column(nullable = false,name = "event_id")
    private Long eventId;

    private String colorHex;

    @Column(nullable = false)
    private Long totalBudget;

    @Column(nullable = false)
    private Long remainingBudget;

    @Column(nullable = false)
    private Long spent;

    @Version
    private Long version;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "assignedTeam")
    private List<AuctionPlayer> players;

    @Column(name = "captain_nomination")
    private Boolean captainNomination = false;

    @Column(name = "captain_confirmation")
    private Boolean captainConfirmation = false;

    private LocalDateTime createdAt;
    
    @PrePersist  void onCreate() { createdAt = LocalDateTime.now(); }
}


