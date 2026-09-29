package com.manacommunity.sports.service.sample.data;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Role;
import com.manacommunity.common.user.model.AppUser;

import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AuctionSeeder — Seeds tournament auction setups, categories, dispute committees, teams, and queues auction players.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSeeder {

    private final AuctionConfigRepository configRepo;
    private final AuctionConfigCategoryRepository configCategoryRepo;
    private final AuctionDisputeCommitteeRepository committeeRepo;
    private final AuctionTeamRepository teamRepo;
    private final AuctionPlayerRepository playerRepo;
    private final SportsEventRegistrationRepository regRepo;

    private final SportsMetaSeeder sportsMetaSeeder;
    private final SportsEventSeeder sportsEventSeeder;
    private final UserSeeder userSeeder;

    @Transactional
    public void seed() {
        log.info("Seeding auction configurations...");

        SportsMeta cricket = sportsMetaSeeder.getOrCreateSport("Cricket", "🏏");
        SportsEvent summerCup = sportsEventSeeder.getSummerCup();
        AppUser superAdmin = userSeeder.getSuperAdmin();
        AppUser sunil = userSeeder.getSunil();
        AppUser ramesh = userSeeder.getRamesh();
        AppUser user1 = userSeeder.getUser1();
        AppUser user6 = userSeeder.getUserByEmail("vikram.singh@gmail.com");
        AppUser user8 = userSeeder.getUserByEmail("rohit.verma@gmail.com");

        // 1. Create Auction Configuration
        AuctionConfig auctionConfig = getOrCreateAuctionConfig(
                cricket, summerCup, "Season 2026",
                AuctionConfig.AuctionFormat.OPEN_AUCTION,
                6,          // totalTeams
                30,         // totalPlayers
                100000L,    // budgetPerTeam
                1000,       // basePrice
                1000,       // bidIncrementDefault
                10000L,     // bidIncrementThreshold
                5000,       // bidIncrementAbove
                30,         // bidTimerSeconds
                true,       // rtmEnabled
                AuctionConfig.UnsoldRule.ROTATION_AUCTION,
                AuctionConfig.AuctionStatus.DRAFT,
                superAdmin
        );

        log.info("✓ Auction config seeded: Season 2026 (id={})", auctionConfig.getId());

        // 2. Create Auction Categories
        createConfigCategory(auctionConfig, "Batsmen");
        createConfigCategory(auctionConfig, "Bowler");
        createConfigCategory(auctionConfig, "All-rounder");
        createConfigCategory(auctionConfig, "Wicket Keeper");

        log.info("✓ Auction config categories seeded: Batsmen, Bowlers, All-rounders, Wicket Keepers");

        // 3. Create Dispute Committee Member
        createCommitteeMember(auctionConfig, "Sunil Kanthala", null, "COMMITTEE_MEMBER");

        log.info("✓ Dispute committee seeded: 1 member");

        // 4. Create Auction Teams
        createAuctionTeam(auctionConfig, "Team 1", sunil, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 2", ramesh, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 3", user1, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 4", user6, "", 100000L, true, true, summerCup.getId());
        createAuctionTeam(auctionConfig, "Team 5", user8, "", 100000L, true, true, summerCup.getId());

        log.info("✓ Auction teams seeded: 5 active teams");

        // 5. Create Auction Players from Confirmed Registrations
        createAuctionPlayersFromConfirmedRegistrations(summerCup, auctionConfig);

        log.info("✓ Auction players seeded from confirmed event registrations");
    }

    private AuctionConfig getOrCreateAuctionConfig(SportsMeta sport, SportsEvent event, String seasonName,
                                                   AuctionConfig.AuctionFormat auctionFormat,
                                                   int totalTeams, int totalPlayers, long budgetPerTeam,
                                                   int basePrice, int bidIncrementDefault,
                                                   long bidIncrementThreshold, int bidIncrementAbove,
                                                   int bidTimerSeconds, boolean rtmEnabled,
                                                   AuctionConfig.UnsoldRule unsoldRule,
                                                   AuctionConfig.AuctionStatus status,
                                                   AppUser createdBy) {
        return configRepo.findByEventId(event.getId()).orElseGet(() ->
                configRepo.save(AuctionConfig.builder()
                        .sport(sport)
                        .event(event)
                        .seasonName(seasonName)
                        .auctionFormat(auctionFormat)
                        .totalTeams(totalTeams)
                        .totalPlayers(totalPlayers)
                        .budgetPerTeam(budgetPerTeam)
                        .basePrice(basePrice)
                        .bidIncrementDefault(bidIncrementDefault)
                        .bidIncrementThreshold(bidIncrementThreshold)
                        .bidIncrementAbove(bidIncrementAbove)
                        .bidTimerSeconds(bidTimerSeconds)
                        .rtmEnabled(rtmEnabled)
                        .unsoldRule(unsoldRule)
                        .status(status)
                        .createdBy(createdBy)
                        .build()));
    }

    private void createConfigCategory(AuctionConfig config, String categoryName) {
        boolean exists = configCategoryRepo.findAll().stream()
                .anyMatch(c -> c.getConfig().getId().equals(config.getId())
                        && c.getCategoryName().equals(categoryName));
        if (!exists) {
            configCategoryRepo.save(new AuctionConfigCategory(config, categoryName));
        }
    }

    private void createCommitteeMember(AuctionConfig config, String memberName,
                                       AppUser user, String role) {
        boolean exists = committeeRepo.findAll().stream()
                .anyMatch(c -> c.getConfig().getId().equals(config.getId())
                        && c.getMemberName().equals(memberName));
        if (!exists) {
            committeeRepo.save(AuctionDisputeCommittee.builder()
                    .config(config)
                    .memberName(memberName)
                    .user(user)
                    .role(role)
                    .build());
        }
    }

    private void createAuctionTeam(AuctionConfig config, String teamName, AppUser owner,
                                   String colorHex, long budget,
                                   boolean captainNomination, boolean captainConfirmation, Long eventId) {
        boolean exists = teamRepo.findByConfigIdOrderByTeamName(config.getId()).stream()
                .anyMatch(t -> t.getTeamName().equals(teamName)
                        && t.getOwnerUser() != null
                        && t.getOwnerUser().getId().equals(owner.getId()));
        if (!exists) {
            teamRepo.save(AuctionTeam.builder()
                    .config(config)
                    .teamName(teamName)
                    .captainUser(owner)
                    .ownerUser(owner)
                    .ownerName(owner.getFullName())
                    .colorHex(colorHex)
                    .totalBudget(budget)
                    .remainingBudget(budget)
                    .spent(0L)
                    .eventId(eventId)
                    .captainNomination(captainNomination)
                    .captainConfirmation(captainConfirmation)
                    .build());
        }
    }

    private void createAuctionPlayersFromConfirmedRegistrations(SportsEvent event, AuctionConfig config) {
        List<SportsEventRegistration> confirmedRegs = regRepo.findByEventIdAndStatus(event.getId(), SportsEventRegistration.RegistrationStatus.CONFIRMED);
        int queueOrder = 1;
        for (SportsEventRegistration reg : confirmedRegs) {
            boolean exists = playerRepo.findByConfigId(config.getId()).stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(reg.getUser().getId()));
            if (!exists) {
                String cat = "Batsmen";
                if ("Bowler".equalsIgnoreCase(reg.getRole())) cat = "Bowler";
                else if ("All-rounder".equalsIgnoreCase(reg.getRole())) cat = "All-rounder";
                else if ("Wicket Keeper".equalsIgnoreCase(reg.getRole())) cat = "Wicket Keeper";

                AuctionPlayer player = AuctionPlayer.builder()
                        .config(config)
                        .user(reg.getUser())
                        .playerName(reg.getPlayerName())
                        .category(cat)
                        .playerRole(reg.getRole())
                        .age(reg.getAge())
                        .basePrice(config.getBasePrice() != null ? config.getBasePrice() : 1000)
                        .statsJson("{\"matches\":24,\"runs\":620,\"wickets\":18}")
                        .queueOrder(queueOrder++)
                        .status(AuctionPlayer.PlayerStatus.QUEUED)
                        .uploadedAt(LocalDateTime.now())
                        .build();
                playerRepo.save(player);
            }
        }
    }
}



