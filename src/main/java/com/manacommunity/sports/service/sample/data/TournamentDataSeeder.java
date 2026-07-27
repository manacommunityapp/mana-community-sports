package com.manacommunity.sports.service.sample.data;

import com.manacommunity.sports.model.Tournament;
import com.manacommunity.sports.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * TournamentDataSeeder — dedicated seeder for the {@code tournament} table.
 * Seeds a distinct tournament (idempotent by name) so it coexists with
 * {@link TournamentSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentDataSeeder {

    public static final String TOURNAMENT_NAME = "LE 2026 Winter Cup";

    private final TournamentRepository tournamentRepo;
    private final com.manacommunity.sports.repository.CommunityRepository communityRepo;

    @Transactional
    public void seed() {
        log.info("Seeding tournament table sample data...");

        Tournament tournament = tournamentRepo.findAll().stream()
                .filter(t -> TOURNAMENT_NAME.equalsIgnoreCase(t.getName()))
                .findFirst()
                .orElseGet(() -> tournamentRepo.save(Tournament.builder()
                        .name(TOURNAMENT_NAME)
                        .description("LE 2026 Winter Cup")
                        .eventDateStart(LocalDate.of(2026, 12, 1))
                        .eventDateEnd(LocalDate.of(2026, 12, 15))
                        .registrationDateStart(LocalDate.of(2026, 11, 1))
                        .registrationDateEnd(LocalDate.of(2026, 11, 10))
                        .maxParticipants(32)
                        .contactNumber("9000000002")
                        .contactEmail("wintercup@community.com")
                        .allowAdminChat(false)
                        .startTime("09:00 AM")
                        .dueTime("06:00 PM")
                        .otherContacts("[]")
                        .bannerImage("")
                        .registrationStatus(Tournament.EventStatus.DRAFT)
                        .sportsEvents(new ArrayList<>())
                        .sponsors(new ArrayList<>())
                        .community(communityRepo.findById(2L).orElse(null))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        log.info("✓ Tournament table seeded: {} (id={})", TOURNAMENT_NAME, tournament.getId());
    }
}
