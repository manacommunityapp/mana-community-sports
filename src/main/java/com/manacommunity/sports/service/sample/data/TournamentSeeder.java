package com.manacommunity.sports.service.sample.data;

import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.Tournament;
import com.manacommunity.sports.repository.CommunityRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import com.manacommunity.sports.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentSeeder {

    private final TournamentRepository tournamentRepo;
    private final SportsEventRepository sportsEventRepo;
    private final CommunityRepository communityRepo;

    @Transactional
    public void seed() {
        log.info("Seeding tournament sample data...");

        SportsEvent badmintonEvent = sportsEventRepo.findAll().stream()
                .filter(e -> e.getName().equalsIgnoreCase("Badminton — Men's Above 19"))
                .findFirst()
                .orElse(null);

        Tournament tournament = tournamentRepo.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase("LE 2026 Summer Champ"))
                .findFirst()
                .orElseGet(() -> {
                    Tournament newT = Tournament.builder()
                            .name("LE 2026 Summer Champ")
                            .description("LE 2026 Summer Champ")
                            .eventDateStart(LocalDate.of(2026, 6, 14))
                            .eventDateEnd(LocalDate.of(2026, 6, 16))
                            .registrationDateStart(LocalDate.of(2026, 6, 1))
                            .registrationDateEnd(LocalDate.of(2026, 6, 2))
                            .maxParticipants(64)
                            .contactNumber("9852367412")
                            .contactEmail("sunil@gmail.com")
                            .allowAdminChat(false)
                            .startTime("09:00 AM")
                            .dueTime("06:00 PM")
                            .otherContacts("[]")
                            .bannerImage("")
                            .registrationStatus(Tournament.EventStatus.REGISTRATION_CLOSED)
                            .sportsEvents(new ArrayList<>())
                            .sponsors(new ArrayList<>())
                            .community(communityRepo.findById(2L).orElse(null))
                            .createdAt(LocalDateTime.of(2026, 6, 1, 0, 5, 7))
                            .updatedAt(LocalDateTime.of(2026, 6, 1, 0, 10, 45))
                            .build();
                    
                    return tournamentRepo.save(newT);
                });

        if (badmintonEvent != null && badmintonEvent.getTournament() == null) {
            badmintonEvent.setTournament(tournament);
            sportsEventRepo.save(badmintonEvent);
            
            if (tournament.getSportsEvents() == null) {
                tournament.setSportsEvents(new ArrayList<>());
            }
            if (!tournament.getSportsEvents().contains(badmintonEvent)) {
                tournament.getSportsEvents().add(badmintonEvent);
                tournamentRepo.save(tournament);
            }
            log.info("✓ Linked sports event 'Badminton — Men's Above 19' to tournament 'LE 2026 Summer Champ'");
        }

        log.info("✓ Tournament seeded: LE 2026 Summer Champ (id={})", tournament.getId());
    }
}
