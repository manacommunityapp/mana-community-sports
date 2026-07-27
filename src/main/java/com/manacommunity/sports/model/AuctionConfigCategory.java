package com.manacommunity.sports.model;

import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table(name = "auction_config_category")
@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class AuctionConfigCategory {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id", nullable = false)
    private AuctionConfig config;

    @Column(nullable = false)
    private String categoryName;

    public AuctionConfigCategory(AuctionConfig config, String categoryName) {
        this.config = config;
        this.categoryName = categoryName;
    }
}
