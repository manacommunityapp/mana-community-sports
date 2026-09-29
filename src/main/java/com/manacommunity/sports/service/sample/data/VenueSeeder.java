package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.Venue;
import com.manacommunity.sports.model.Court;
import com.manacommunity.sports.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

/**
 * VenueSeeder — Seeds local community sports venues.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueSeeder {

    private final VenueRepository venueRepo;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void defaultSeed() {
        log.info("Seeding community venues...");
        Community leCommunity = communitySeeder.getLeCommunity();

//        getOrCreateVenue(
//                "LE Box Cricket", "COMMUNITY", leCommunity,
//                "Hyderabad", "Coomunity Back Gate", "Miyapur");

        log.info("✓ Venues seeded successfully.");
    }

    @Transactional
    public void seed() {
        log.info("Seeding community venues...");
        Community leCommunity = communitySeeder.getLeCommunity();

        getOrCreateVenue(
                "LE Box Cricket", "COMMUNITY", leCommunity,
                "Hyderabad", "Coomunity Back Gate", "Miyapur",
                "500049", 2, "COMMUNITY",
                "10:00 AM", "08:00 PM",
                "sunil", "987463214", "sunil@gmail.com");

        List<Court> badmintonCourts = List.of(
                Court.builder().name("Court 1").color("#3b82f6").build(),
                Court.builder().name("Court 2").color("#f73bb2").build()
        );

        getOrCreateVenue(
                "LE Badminton Court", "APARTMENT", leCommunity,
                "hyderabad", "beside super market", "miyapur",
                "500049", 2, "COMMUNITY",
                "10:00 AM", "08:00 PM",
                "sunil", "987463214", "sunil@gmail.com",
                badmintonCourts
        );

        log.info("✓ Venues seeded successfully.");
    }

    public Venue getLeBoxCricket() {
        return venueRepo.findAll().stream()
                .filter(v -> v.getName().equals("LE Box Cricket"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("LE Box Cricket venue has not been seeded yet."));
    }

    public Venue getLeBadmintonCourt() {
        return venueRepo.findAll().stream()
                .filter(v -> v.getName().equals("LE Badminton Court"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("LE Badminton Court venue has not been seeded yet."));
    }

    private Venue getOrCreateVenue(String name, String venueType, Community community,
                                   String city, String address, String area) {
        return getOrCreateVenue(name, venueType, community, city, address, area,
                "500049", 500, community != null ? community.getType() : "GENERAL",
                "08:00 AM", "10:00 PM", null, null, null, null);
    }

    private Venue getOrCreateVenue(String name, String venueType, Community community,
                                   String city, String address, String area,
                                   String pinCode, Integer capacity, String venueCategory,
                                   String openingTime, String closingTime,
                                   String contactName, String contactNumber, String contactEmail) {
        return getOrCreateVenue(name, venueType, community, city, address, area,
                pinCode, capacity, venueCategory, openingTime, closingTime,
                contactName, contactNumber, contactEmail, null);
    }

    private Venue getOrCreateVenue(String name, String venueType, Community community,
                                   String city, String address, String area,
                                   String pinCode, Integer capacity, String venueCategory,
                                   String openingTime, String closingTime,
                                   String contactName, String contactNumber, String contactEmail,
                                   List<Court> courts) {
        Venue venue = venueRepo.findAll().stream()
                .filter(v -> v.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Venue newVenue = Venue.builder()
                            .name(name)
                            .venueType(venueType)
                            .address(address)
                            .area(area)
                            .venueCategory(venueCategory)
                            .city(city)
                            .pinCode(pinCode)
                            .capacity(capacity)
                            .community(community)
                            .openingTime(openingTime)
                            .closingTime(closingTime)
                            .contactName(contactName)
                            .contactNumber(contactNumber)
                            .contactEmail(contactEmail)
                            .build();
                    return venueRepo.save(newVenue);
                });

        // Ensure courts are populated and saved
        if (courts != null && !courts.isEmpty()) {
            boolean hasCourts = venue.getCourts() != null && !venue.getCourts().isEmpty();
            if (!hasCourts) {
                if (venue.getCourts() == null) {
                    venue.setCourts(new ArrayList<>());
                }
                for (Court c : courts) {
                    c.setVenue(venue);
                    venue.getCourts().add(c);
                }
                venueRepo.save(venue);
            }
        }
        return venue;
    }
}


