package com.manacommunity.sports.model;

import com.manacommunity.common.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "court")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "venue")
@EqualsAndHashCode(exclude = "venue")
public class Court {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String color; // e.g., "#3b82f6"

    private String openingTime;

    private String closingTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    @JsonIgnore
    private Venue venue;
}

