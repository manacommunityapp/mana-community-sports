package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

/**
 * BUG FIX: PlayerCategory was an empty stub class.
 * Now fully mapped to the player_category table.
 */
@Entity
@Table(name = "player_category")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @JsonProperty("categoryType")
    @Column(nullable = false, unique = false, length = 10)
    private String category_type; // MENS, WOMENS, BOYS, GIRLS, KIDS, SENIORS

    @Column(length = 200)
    private String description;

    @Column(nullable = false,name = "min_age")
    private Integer minAge;

    @Column(nullable = false,name = "max_age")
    private Integer maxAge;

    @Column(nullable = false,unique = false, length = 10)
    private String gender; // MALE, FEMALE, ALL

    @Column(nullable = false,unique = false, length = 10)
    private String type; // DEFAULT, USER, VENDOR

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;
}


