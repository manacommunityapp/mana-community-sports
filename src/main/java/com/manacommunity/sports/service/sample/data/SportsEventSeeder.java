package com.manacommunity.sports.service.sample.data;

import com.manacommunity.sports.model.Community;

import com.manacommunity.sports.user.model.AppUser;

import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import com.manacommunity.sports.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * SportsEventSeeder — Seeds community tournaments and handles user event registrations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventSeeder {

    private final SportsEventRepository sportsEventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final TournamentRepository tournamentRepo;
    
    private final CommunitySeeder communitySeeder;
    private final SportsMetaSeeder sportsMetaSeeder;
    private final VenueSeeder venueSeeder;
    private final UserSeeder userSeeder;
    private final PlayerCategorySeeder playerCategorySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding community sports events...");
        
        SportsMeta cricket = sportsMetaSeeder.getOrCreateSport("Cricket", "🏏");
        Community leCommunity = communitySeeder.getLeCommunity();
        Venue leBoxCricket = venueSeeder.getLeBoxCricket();
        AppUser ramesh = userSeeder.getRamesh();
        
        PlayerCategory boysU19 = playerCategorySeeder.getCategoryByName("Boy's Under 19");
        PlayerCategory mensA19 = playerCategorySeeder.getCategoryByName("Men's Above 19");

        SportsEvent summerCup = getOrCreateSportsEvent(
                "Annual Summer Cricket Cup",
                true,
                cricket, leCommunity, leBoxCricket, ramesh, Set.of(boysU19, mensA19),
                SportsEvent.EventStatus.REGISTRATION_OPEN,
                List.of("SINGLES", "DOUBLES", "MIXED_DOUBLES", "TEAM"),
                SportsEvent.TournamentType.KNOCKOUT,
                LocalDate.of(2026, 5, 22),
                LocalDate.of(2026, 5, 24),
                LocalDate.of(2026, 5, 16),
                LocalDate.of(2026, 5, 18),
                100,
                18,45,
                "MALE",
                LocalDate.of(1900, 1, 1),
                "3,4" // dispute committee: Sunil(3), Ramesh(4)
        );

        log.info("✓ Sports events seeded: Annual Summer Cricket Cup (id={})", summerCup.getId());

        SportsMeta badminton = sportsMetaSeeder.getOrCreateSport("Badminton", "🏸");
        Venue leBadmintonCourt = venueSeeder.getLeBadmintonCourt();

        SportsEvent badmintonEvent = getOrCreateSportsEvent(
                "Badminton — Men's Above 19",
                true,
                badminton, leCommunity, leBadmintonCourt, ramesh, Set.of(mensA19),
                SportsEvent.EventStatus.DRAFT,
                List.of("SINGLES", "DOUBLES"),
                SportsEvent.TournamentType.KNOCKOUT_SINGLE,
                LocalDate.of(2026, 6, 3),
                LocalDate.of(2026, 6, 6),
                null,
                null,
                60,
                19, 45,
                "MALE",
                LocalDate.of(1900, 1, 1),
                null
        );

        log.info("✓ Sports events seeded: Badminton — Men's Above 19 (id={})", badmintonEvent.getId());

        // ════════════════════════════════════════════════════════════════════
        // SPORTS EVENT REGISTRATIONS
        // ════════════════════════════════════════════════════════════════════
        createRegistration(summerCup, userSeeder.getSandeep(), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Sandeep Kamarapu", 36, "All-rounder");
        createRegistration(summerCup, userSeeder.getSunil(), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Sunil Kanthala", 36, "Batsman");
        createRegistration(summerCup, ramesh, mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "Ramesh Korlakunta", 36, "Bowler");
        createRegistration(summerCup, userSeeder.getUser1(), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.REGISTERED, "user1", 36, "Wicket Keeper");

        // Block A
        createRegistration(summerCup, userSeeder.getUserByEmail("rahul.sharma@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rahul Sharma", 38, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("amit.kumar@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Amit Kumar", 41, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("vikram.singh@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vikram Singh", 36, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("rohit.verma@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rohit Verma", 44, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("karan.malhotra@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Karan Malhotra", 32, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("suresh.nair@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Suresh Nair", 48, "Wicket Keeper");

        // Block B
        createRegistration(summerCup, userSeeder.getUserByEmail("rajat.bhatia@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Rajat Bhatia", 30, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("deepak.pillai@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Deepak Pillai", 46, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("manish.tiwari@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Manish Tiwari", 39, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("arjun.kapoor@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Arjun Kapoor", 35, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("tarun.garg@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Tarun Garg", 28, "Wicket Keeper");
        createRegistration(summerCup, userSeeder.getUserByEmail("nitin.das@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Nitin Das", 47, "All-rounder");

        // Block C
        createRegistration(summerCup, userSeeder.getUserByEmail("siddharth.bose@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Siddharth Bose", 33, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("varun.mehta@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Varun Mehta", 38, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("gourav.pandey@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Gourav Pandey", 31, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("abhishek.mishra@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Abhishek Mishra", 40, "Wicket Keeper");
        createRegistration(summerCup, userSeeder.getUserByEmail("vishal.shetty@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Vishal Shetty", 43, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("prashant.kadam@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Prashant Kadam", 49, "Bowler");

        // Block D
        createRegistration(summerCup, userSeeder.getUserByEmail("harsh.vardhan@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Harsh Vardhan", 35, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("yash.chopra@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Yash Chopra", 44, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("akash.ambani@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Akash Ambani", 36, "Bowler");
        createRegistration(summerCup, userSeeder.getUserByEmail("naveen.kumar@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Naveen Kumar", 41, "All-rounder");
        createRegistration(summerCup, userSeeder.getUserByEmail("sanjay.dutt@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Sanjay Dutt", 51, "Wicket Keeper");
        createRegistration(summerCup, userSeeder.getUserByEmail("mahesh.babu@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Mahesh Babu", 46, "Batsman");
        createRegistration(summerCup, userSeeder.getUserByEmail("ajay.devgn@gmail.com"), mensA19, SportsEvent.MatchFormat.SINGLES, SportsEventRegistration.RegistrationStatus.CONFIRMED, "Ajay Devgn", 48, "Bowler");

        log.info("✓ Registrations seeded: 31 confirmed players (Men above 19) for Annual Summer Cricket Cup");
    }

    public SportsEvent getSummerCup() {
        return sportsEventRepo.findAll().stream()
                .filter(e -> e.getName().equalsIgnoreCase("Annual Summer Cricket Cup"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Annual Summer Cricket Cup has not been seeded yet."));
    }

    private SportsEvent getOrCreateSportsEvent(String name, boolean activeStatus,SportsMeta sport, Community community,
                                               Venue venue, AppUser createdBy, Set<PlayerCategory> categories,
                                               SportsEvent.EventStatus status,
                                               List<String> formats,
                                               SportsEvent.TournamentType tournamentType,
                                               LocalDate dateStart, LocalDate dateEnd,
                                               LocalDate regDateStart, LocalDate regDateEnd,
                                               int maxParticipants,
                                               int minAge, int maxAge,
                                               String gender,
                                               LocalDate playersBorn,
                                               String ignoredDisputeCommitteeIds) {
        return sportsEventRepo.findAll().stream()
                .filter(e -> e.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    SportsEvent saved = sportsEventRepo.save(SportsEvent.builder()
                            .name(name)
                            .active(activeStatus)
                            .sport(sport)
                            .community(community)
                            .venue(venue)
                            .createdBy(createdBy)
                            .status(status)
                            .format(formats != null ? formats : java.util.Collections.emptyList())
                            .tournamentType(tournamentType)
                            .registrationDateStart(regDateStart)
                            .registrationDateEnd(regDateEnd)
                            .eventDateStart(dateStart)
                            .eventDateEnd(dateEnd)
                            .maxParticipants(maxParticipants)
                            .categories(categories)
                            .minAge(minAge)
                            .maxAge(maxAge)
                            .gender(gender)
                            .playersBorn(playersBorn)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build());

//                    // Save corresponding Tournament record to tournament table
//                    Tournament.MatchFormat matchFormat = null;
//                    if (saved.getFormat() != null && !saved.getFormat().isEmpty()) {
//                        try {
//                            matchFormat = Tournament.MatchFormat.valueOf(saved.getFormat().get(0));
//                        } catch (Exception ignored) {}
//                    }
//                    Tournament.TournamentType tType = null;
//                    if (saved.getTournamentType() != null) {
//                        try {
//                            tType = Tournament.TournamentType.valueOf(saved.getTournamentType().name());
//                        } catch (Exception ignored) {}
//                    }
//
//                    Tournament tournament = Tournament.builder()
//                            .name(saved.getName())
//                            .event(saved)
//                            .format(matchFormat)
//                            .tournamentType(tType)
//                            .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now())
//                            .updatedAt(saved.getUpdatedAt() != null ? saved.getUpdatedAt() : LocalDateTime.now())
//                            .build();
//                    tournamentRepo.save(tournament);

                    return saved;
                });
    }

    private void createRegistration(SportsEvent event, AppUser user, PlayerCategory category,
                                    SportsEvent.MatchFormat matchType,
                                    SportsEventRegistration.RegistrationStatus status,
                                    String playerName, int age, String role) {
        if (regRepo.existsByEventIdAndUserIdAndPlayerName(event.getId(), user.getId(), playerName)) {
            return; // Already exists, skip
        }
        regRepo.save(SportsEventRegistration.builder()
                .event(event)
                .user(user)
                .category(category)
                .matchType(matchType)
                .status(status)
                .playerName(playerName)
                .age(age)
                .flatNumber(user.getBlock() + " " + user.getFlatNo())
                .role(role)
                .registeredAt(LocalDateTime.now())
                .build());
    }
}
