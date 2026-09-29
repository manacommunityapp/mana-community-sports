package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;

import com.manacommunity.common.user.model.AppUser;

import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.SportsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * SportsEventDataSeeder — dedicated seeder for the {@code sports_event} table.
 * Seeds a distinct event (idempotent by name) so it coexists with
 * {@link SportsEventSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventDataSeeder {

    public static final String EVENT_NAME = "Winter Football League";

    private final SportsEventRepository sportsEventRepo;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final CommunitySeeder communitySeeder;
    private final VenueDataSeeder venueDataSeeder;
    private final UserSeeder userSeeder;
    private final PlayerCategorySeeder playerCategorySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding sports_event table sample data...");
        getOrCreateWinterLeague();
        log.info("✓ Sports event table seeded: {}", EVENT_NAME);
    }

    /** Find-or-create the Winter Football League event (idempotent by name). */
    public SportsEvent getOrCreateWinterLeague() {
        SportsMeta football = sportsMetaSeeder.getOrCreateSport("Football", "⚽");
        Community community = communitySeeder.getLeCommunity();
        Venue arena = venueDataSeeder.getOrCreateArena();
        AppUser createdBy = userSeeder.getRamesh();
        PlayerCategory mensA19 = playerCategorySeeder.getCategoryByName("Men's Above 19");

        return sportsEventRepo.findAll().stream()
                .filter(e -> EVENT_NAME.equalsIgnoreCase(e.getName()))
                .findFirst()
                .orElseGet(() -> sportsEventRepo.save(SportsEvent.builder()
                        .name(EVENT_NAME)
                        .active(true)
                        .sport(football)
                        .community(community)
                        .venue(arena)
                        .createdBy(createdBy)
                        .format(List.of("TEAM"))
                        .tournamentType(SportsEvent.TournamentType.ROUND_ROBIN)
                        .registrationDateStart(LocalDate.of(2026, 11, 1))
                        .registrationDateEnd(LocalDate.of(2026, 11, 10))
                        .eventDateStart(LocalDate.of(2026, 12, 1))
                        .eventDateEnd(LocalDate.of(2026, 12, 15))
                        .maxParticipants(32)
                        .categories(Set.of(mensA19))
                        .minAge(19)
                        .maxAge(45)
                        .gender("MALE")
                        .playersBorn(LocalDate.of(1900, 1, 1))
                        .status(SportsEvent.EventStatus.DRAFT)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }
}


