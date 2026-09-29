package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.Court;
import com.manacommunity.sports.model.Venue;
import com.manacommunity.sports.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CourtDataSeeder — dedicated seeder for the {@code court} table. Adds courts
 * to the {@link VenueDataSeeder} arena (idempotent by court name per venue).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourtDataSeeder {

    private final CourtRepository courtRepo;
    private final VenueDataSeeder venueDataSeeder;

    @Transactional
    public void seed() {
        log.info("Seeding court table sample data...");
        Venue arena = venueDataSeeder.getOrCreateArena();

        List<Court> courts = List.of(
                Court.builder().name("Arena Court A").color("#3b82f6").build(),
                Court.builder().name("Arena Court B").color("#f59e0b").build(),
                Court.builder().name("Arena Court C").color("#10b981").build()
        );

        List<Court> existing = courtRepo.findByVenueId(arena.getId());
        int created = 0;
        for (Court c : courts) {
            boolean present = existing.stream().anyMatch(e -> e.getName().equalsIgnoreCase(c.getName()));
            if (!present) {
                c.setVenue(arena);
                courtRepo.save(c);
                created++;
            }
        }
        log.info("✓ Court table seeded: {} new court(s) for {}", created, arena.getName());
    }
}

