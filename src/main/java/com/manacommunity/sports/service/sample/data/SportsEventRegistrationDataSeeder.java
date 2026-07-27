package com.manacommunity.sports.service.sample.data;

import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.SportsEventRegistration;
import com.manacommunity.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SportsEventRegistrationDataSeeder — dedicated seeder for the
 * {@code sports_event_registration} table. Registers players into
 * events (idempotent per event/user/playerName) so it coexists
 * with {@link SportsEventSeeder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventRegistrationDataSeeder {

    private final SportsEventRegistrationRepository regRepo;
    private final SportsEventRepository sportsEventRepo;
    private final SportsEventDataSeeder sportsEventDataSeeder;
    private final UserSeeder userSeeder;
    private final PlayerCategorySeeder playerCategorySeeder;

    @Transactional
    public void seed() {
        log.info("Seeding sports_event_registration table sample data...");

        // ── Winter Football League registrations (existing) ──────────────
        seedWinterLeagueRegistrations();

        // ── Badminton — Men's Above 19 registrations (from CSV) ──────────
        seedBadmintonRegistrations();
    }

    /**
     * Seeds 3 registrations for the Winter Football League (existing logic).
     */
    private void seedWinterLeagueRegistrations() {
        SportsEvent event = sportsEventDataSeeder.getOrCreateWinterLeague();
        PlayerCategory mensA19 = playerCategorySeeder.getCategoryByName("Men's Above 19");

        int created = 0;
        created += register(event, userSeeder.getSandeep(), mensA19,
                SportsEvent.MatchFormat.TEAM,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Sandeep (Winter)", 36, "Striker", null);
        created += register(event, userSeeder.getSunil(), mensA19,
                SportsEvent.MatchFormat.TEAM,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Sunil (Winter)", 36, "Midfielder", null);
        created += register(event, userSeeder.getRamesh(), mensA19,
                SportsEvent.MatchFormat.TEAM,
                SportsEventRegistration.RegistrationStatus.REGISTERED,
                "Ramesh (Winter)", 36, "Defender", null);

        log.info("✓ Sports event registration table seeded: {} new registration(s) for {}",
                created, event.getName());
    }

    /**
     * Seeds 16 badminton participant registrations for the
     * "Badminton — Men's Above 19" event based on CSV data.
     * <p>
     * Source: sample_participants.csv
     * All participants are registered in the "Men's Single" category
     * under the "Men's Above 19" PlayerCategory.
     */
    private void seedBadmintonRegistrations() {
        SportsEvent badmintonEvent = sportsEventRepo.findAll().stream()
                .filter(e -> "Badminton — Men's Above 19".equals(e.getName()))
                .findFirst()
                .orElse(null);

        if (badmintonEvent == null) {
            log.warn("⚠ Badminton — Men's Above 19 event not found. Skipping badminton registrations.");
            return;
        }

        PlayerCategory mensA19 = playerCategorySeeder.getCategoryByName("Men's Above 19");

        int created = 0;

        // ── CSV Data: 16 participants for Badminton — Men's Above 19 ──
        created += register(badmintonEvent, userSeeder.getSandeep(), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Sandeep Kamarapu", 35, null, "B-402");

        created += register(badmintonEvent, userSeeder.getSunil(), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Sunil Kanthala", 30, null, "C-101");

        created += register(badmintonEvent, userSeeder.getRamesh(), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Ramesh Korlakunta", 26, null, "C-102");

        created += register(badmintonEvent, userSeeder.getMady(), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.REGISTERED,
                "Mady", 28, null, "A-103");

        created += register(badmintonEvent, userSeeder.getUser1(), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "user1", 28, null, "C-104");

        created += register(badmintonEvent, userSeeder.getUserByEmail("rahul.sharma@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Rahul Sharma", 28, null, "D-105");

        created += register(badmintonEvent, userSeeder.getUserByEmail("priya.patel@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Priya Patel", 28, null, "C-106");

        created += register(badmintonEvent, userSeeder.getUserByEmail("amit.kumar@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Amit Kumar", 40, null, "C-107");

        created += register(badmintonEvent, userSeeder.getUserByEmail("sneha.reddy@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Sneha Reddy", 28, null, "A-108");

        created += register(badmintonEvent, userSeeder.getUserByEmail("vikram.singh@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Vikram Singh", 28, null, "C-109");

        created += register(badmintonEvent, userSeeder.getUserByEmail("ananya.desai@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Ananya Desai", 28, null, "C-110");

        created += register(badmintonEvent, userSeeder.getUserByEmail("rohit.verma@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Rohit Verma", 36, null, "D-111");

        created += register(badmintonEvent, userSeeder.getUserByEmail("neha.gupta@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Neha Gupta", 28, null, "C-112");

        created += register(badmintonEvent, userSeeder.getUserByEmail("karan.malhotra@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Karan Malhotra", 28, null, "B-113");

        created += register(badmintonEvent, userSeeder.getUserByEmail("pooja.joshi@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Pooja Joshi", 28, null, "C-114");

        created += register(badmintonEvent, userSeeder.getUserByEmail("suresh.nair@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Suresh Nair", 32, null, "B-115");

        created += register(badmintonEvent, userSeeder.getUserByEmail("Bhupal@gmail.com"), mensA19,
                SportsEvent.MatchFormat.SINGLES,
                SportsEventRegistration.RegistrationStatus.CONFIRMED,
                "Bhupal", 38, null, "B-209");


        log.info("✓ Badminton registration seeded: {} new registration(s) for {}",
                created, badmintonEvent.getName());
    }

    /**
     * Idempotent registration helper. Checks existence by event + user + playerName
     * before inserting.
     *
     * @param flatNumberOverride if non-null, uses this flat number from CSV;
     *                           otherwise derives from user's block + flatNo.
     */
    private int register(SportsEvent event, AppUser user, PlayerCategory category,
                         SportsEvent.MatchFormat matchType,
                         SportsEventRegistration.RegistrationStatus status,
                         String playerName, int age, String role,
                         String flatNumberOverride) {
        if (regRepo.existsByEventIdAndUserIdAndPlayerName(event.getId(), user.getId(), playerName)) {
            return 0; // already exists
        }
        String flatNumber = flatNumberOverride != null
                ? flatNumberOverride
                : (user.getBlock() + " " + user.getFlatNo());

        regRepo.save(SportsEventRegistration.builder()
                .event(event)
                .user(user)
                .category(category)
                .matchType(matchType)
                .status(status)
                .playerName(playerName)
                .age(age)
                .flatNumber(flatNumber)
                .role(role)
                .registeredAt(LocalDateTime.now())
                .build());
        return 1;
    }
}
