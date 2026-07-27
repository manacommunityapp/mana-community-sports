package com.manacommunity.sports.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "venue")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    private String area;

    private String pinCode;

    private String mapLink;

    private Integer capacity;

    private String venueType; // APARTMENT, COLLEGE, SCHOOL, OFFICE, CLUB, OUTSIDE

    private String venueCategory; // Community name or SPORTS_VENUE, PUBLIC_PARK, etc.

    private String openingTime; // e.g. "08:00 AM"

    private String closingTime; // e.g. "08:00 PM"

    private String contactName;

    private String contactNumber;

    private String contactEmail;

    private String contactTitle;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "venue_contact",
        joinColumns = @JoinColumn(name = "venue_id"),
        inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    @Builder.Default
    private List<Contact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<Court> courts = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = true)
    @JsonIgnore
    private Community community;
}
