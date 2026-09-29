package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.user.model.AppUser;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity 
@Table(name = "auction_dispute_committee")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class AuctionDisputeCommittee {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private AuctionConfig config;

    @Column(nullable = false)
    private String memberName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    private String role;
    private LocalDateTime addedAt;
    
    @PrePersist void onCreate() { addedAt = LocalDateTime.now(); }
}



