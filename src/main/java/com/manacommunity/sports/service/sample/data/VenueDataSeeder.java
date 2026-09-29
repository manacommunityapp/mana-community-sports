package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.Venue;
import com.manacommunity.sports.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VenueDataSeeder — dedicated seeder for the {@code venue} table. Seeds a
 * distinct venue (idempotent by name) so it coexists with {@link VenueSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VenueDataSeeder {

    public static final String VENUE_NAME = "Community Sports Arena";

    private final VenueRepository venueRepo;
    private final CommunitySeeder communitySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding venue table sample data...");
        getOrCreateArena();
        log.info("✓ Venue table seeded: {}", VENUE_NAME);
    }

    /** Find-or-create the arena venue (idempotent by name). */
    public Venue getOrCreateArena() {
        Community community = communitySeeder.getLeCommunity();
        return venueRepo.findAll().stream()
                .filter(v -> VENUE_NAME.equalsIgnoreCase(v.getName()))
                .findFirst()
                .orElseGet(() -> venueRepo.save(Venue.builder()
                        .name(VENUE_NAME)
                        .venueType("COMMUNITY")
                        .venueCategory("SPORTS_VENUE")
                        .address("Near Central Park")
                        .area("Gachibowli")
                        .city("Hyderabad")
                        .pinCode("500032")
                        .capacity(500)
                        .community(community)
                        .openingTime("06:00 AM")
                        .closingTime("10:00 PM")
                        .contactName("Arena Desk")
                        .contactNumber("9000000001")
                        .contactEmail("arena@community.com")
                        .build()));
    }
}


