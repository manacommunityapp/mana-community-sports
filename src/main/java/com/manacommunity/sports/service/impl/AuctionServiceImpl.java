package com.manacommunity.sports.service.impl;

import com.manacommunity.sports.security.AuditAction;

import com.manacommunity.sports.security.AuditModule;

import com.manacommunity.sports.security.AuditService;

import com.manacommunity.sports.user.repository.AppUserRepository;

import com.manacommunity.sports.dto.*;
import com.manacommunity.sports.exception.AuctionStateException;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.*;
import com.manacommunity.sports.service.AuctionService;
import com.manacommunity.sports.service.AuctionWebSocketService;
import com.manacommunity.sports.service.NotificationManagementService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionConfigRepository configRepo;
    private final AuctionPlayerRepository playerRepo;
    private final AuctionBidRepository bidRepo;
    private final AuctionTeamRepository teamRepo;
    private final AuctionSessionLogRepository logRepo;
    private final SportMetaRepository sportRepo;
    private final AppUserRepository userRepo;
    private final AuctionConfigCategoryRepository auctionConfigCategoryRepo;
    private final AuctionDisputeCommitteeRepository committeeRepo;
    private final com.manacommunity.sports.repository.SportsEventRegistrationRepository registrationRepo;
    private final com.manacommunity.sports.repository.SportsEventRepository eventRepo;
    private final com.manacommunity.sports.security.AuditService auditService;
    private final MeterRegistry meterRegistry;
    private final NotificationManagementService notificationService;
    private final AuctionWebSocketService auctionWs;

    @Override
    public List<AuctionConfig> getConfigsBySportAndCommunity(Long sportId, Long communityId) {
        if (communityId == null) {
            return configRepo.findBySportIdOrderByCreatedAtDesc(sportId);
        }
        return configRepo.findBySportIdAndCreatedByCommunityIdOrderByCreatedAtDesc(sportId, communityId);
    }
    @Override
    public List<AuctionConfig> getAllConfigsByCommunity(Long communityId) {
        if (communityId == null) return List.of();
        return configRepo.findByCreatedByCommunityIdOrderByCreatedAtDesc(communityId);
    }
    @Override
    @Transactional(readOnly = true)
    public AuctionConfigResponse getConfigResponse(Long id) {
        AuctionConfig config = configRepo.findById(id).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionConfig", id));
        return toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionConfigResponse> getConfigResponsesBySportAndCommunity(Long sportId, Long communityId) {
        List<AuctionConfig> configs = communityId == null
                ? configRepo.findBySportIdWithAssociations(sportId)
                : configRepo.findBySportIdAndCommunityIdWithAssociations(sportId, communityId);
        return configs.stream().map(this::toConfigResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionConfigResponse> getConfigResponsesByCommunity(Long communityId) {
        if (communityId == null) return List.of();
        return configRepo.findByCommunityIdWithAssociations(communityId).stream()
                .map(this::toConfigResponse).toList();
    }

    private AuctionConfigResponse toConfigResponse(AuctionConfig config) {
        return new AuctionConfigResponse(
                config.getId(),
                config.getEvent() != null ? config.getEvent().getId() : null,
                config.getEvent() != null ? config.getEvent().getName() : null,
                config.getSport().getName(),
                config.getSeasonName(),
                config.getAuctionFormat().name(),
                config.getTotalTeams(),
                config.getTotalPlayers(),
                config.getBudgetPerTeam(),
                config.getBasePrice(),
                config.getBidIncrementDefault(),
                config.getBidIncrementThreshold(),
                config.getBidIncrementAbove(),
                config.getBidTimerSeconds(),
                config.getRtmEnabled(),
                config.getUnsoldRule().name(),
                config.getStatus().name(),
                config.getPlayers().stream().map(AuctionPlayer::getCategory).distinct().collect(Collectors.toList()),
                config.getCommitteeMembers().stream().map(AuctionDisputeCommittee::getMemberName).collect(Collectors.toList())
        );
    }

    // ── CREATE AUCTION CONFIG ─────────────────────────────────────
    @Override
    @Transactional
    public AuctionConfig createConfig(AuctionConfigRequest req, Long adminUserId) {
        if (configRepo.existsBySportIdAndSeasonName(req.sportId(), req.seasonName()))
            throw new AuctionStateException("Auction already exists for this sport and season");

        AuctionConfig config = AuctionConfig.builder()
            .sport(sportRepo.findById(req.sportId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("Sport", req.sportId())))
            .event(req.eventId() != null ? eventRepo.findById(req.eventId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("SportsEvent", req.eventId())) : null)
            .seasonName(req.seasonName())
            .auctionFormat(AuctionConfig.AuctionFormat.valueOf(req.auctionFormat()))
            .totalTeams(req.totalTeams())
            .totalPlayers(req.totalPlayers())
            .budgetPerTeam(req.budgetPerTeam())
            .basePrice(req.basePrice())
            .bidIncrementDefault(req.bidIncrementDefault())
            .bidIncrementThreshold(req.bidIncrementThreshold())
            .bidIncrementAbove(req.bidIncrementAbove())
            .bidTimerSeconds(req.bidTimerSeconds())
            .rtmEnabled(Boolean.TRUE.equals(req.rtmEnabled()))
            .unsoldRule(AuctionConfig.UnsoldRule.valueOf(
                req.unsoldRule() != null ? req.unsoldRule() : "ROTATION_AUCTION"))
            .status(AuctionConfig.AuctionStatus.DRAFT)
            .createdBy(userRepo.getReferenceById(adminUserId))
            .build();

        AuctionConfig saved = configRepo.save(config);

        // Persist categories
        if (req.categories() != null) {
            req.categories().forEach(cat ->
                auctionConfigCategoryRepo.save(new AuctionConfigCategory(saved, cat)));
        }
        // Persist committee members
        if (req.committeeMembers() != null) {
            req.committeeMembers().forEach(name ->
                committeeRepo.save(AuctionDisputeCommittee.builder()
                    .config(saved).memberName(name).role("COMMITTEE_MEMBER").build()));
        }
        log.info("Auction config created: sport={} season={}", req.sportId(), req.seasonName());
        return saved;
    }

    // ── UPDATE CONFIG (dynamically update pricing/rules) ──────────
    @Override
    @Transactional
    public AuctionConfig updateConfig(Long configId, AuctionConfigRequest req) {
        AuctionConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Auction config not found: " + configId));

        if (config.getStatus() == AuctionConfig.AuctionStatus.LIVE)
            throw new AuctionStateException("Cannot update rules while auction is LIVE. Pause first.");

        // Apply all dynamic rule changes
        if (req.eventId() != null) {
            config.setEvent(eventRepo.findById(req.eventId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("SportsEvent", req.eventId())));
        } else {
            config.setEvent(null);
        }
        config.setTotalTeams(req.totalTeams());
        config.setTotalPlayers(req.totalPlayers());
        config.setBudgetPerTeam(req.budgetPerTeam());
        config.setBasePrice(req.basePrice());
        config.setBidIncrementDefault(req.bidIncrementDefault());
        config.setBidIncrementThreshold(req.bidIncrementThreshold());
        config.setBidIncrementAbove(req.bidIncrementAbove());
        config.setBidTimerSeconds(req.bidTimerSeconds());
        config.setRtmEnabled(Boolean.TRUE.equals(req.rtmEnabled()));
        if (req.unsoldRule() != null)
            config.setUnsoldRule(AuctionConfig.UnsoldRule.valueOf(req.unsoldRule()));

        return configRepo.save(config);
    }

    @Override
    @Transactional
    public AuctionConfig updateStatus(Long configId, String status) {
        AuctionConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new com.manacommunity.sports.exception.ManaCommunityException(
                        "Please configure the auction configuration before starting the auction.",
                        org.springframework.http.HttpStatus.BAD_REQUEST, "CONFIG_NOT_FOUND"));

        AuctionConfig.AuctionStatus newStatus;
        try {
            newStatus = AuctionConfig.AuctionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new AuctionStateException("Invalid auction status: " + status
                    + ". Valid values: " + java.util.Arrays.toString(AuctionConfig.AuctionStatus.values()));
        }

        AuctionConfig.AuctionStatus oldStatus = config.getStatus();
        if (oldStatus == newStatus) return config;

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new AuctionStateException("Cannot transition auction from " + oldStatus + " to " + newStatus + ".");
        }

        if (newStatus == AuctionConfig.AuctionStatus.LIVE || newStatus == AuctionConfig.AuctionStatus.ACTIVE) {
            long teamCount = teamRepo.countByConfigId(configId);
            if (teamCount < 2) {
                throw new AuctionStateException("Cannot start auction: At least 2 teams must be configured.");
            }
            long playerCount = playerRepo.countByConfigId(configId);
            if (playerCount == 0) {
                throw new AuctionStateException("Cannot start auction: Player pool is empty.");
            }
        }

        config.setStatus(newStatus);
        AuctionConfig savedConfig = configRepo.save(config);

        auctionWs.broadcastStatusChange(configId, oldStatus.name(), newStatus.name());

        // Audit the lifecycle transitions only (start / end), not every save.
        if (oldStatus != newStatus) {
            if (newStatus == AuctionConfig.AuctionStatus.LIVE || newStatus == AuctionConfig.AuctionStatus.ACTIVE) {
                auditService.record(
                    com.manacommunity.sports.security.AuditAction.AUCTION_STARTED,
                    com.manacommunity.sports.security.AuditModule.AUCTION,
                    "AuctionConfig", String.valueOf(configId),
                    String.valueOf(oldStatus), String.valueOf(newStatus));
                notifyAuctionTeamOwners(savedConfig, NotificationType.AUCTION_STARTED,
                        "Auction Started — " + savedConfig.getSeasonName(),
                        "The auction is now live!", NotificationPriority.HIGH);
            } else if (newStatus == AuctionConfig.AuctionStatus.COMPLETED) {
                auditService.record(
                    com.manacommunity.sports.security.AuditAction.AUCTION_ENDED,
                    com.manacommunity.sports.security.AuditModule.AUCTION,
                    "AuctionConfig", String.valueOf(configId),
                    String.valueOf(oldStatus), String.valueOf(newStatus));
                notifyAuctionTeamOwners(savedConfig, NotificationType.AUCTION_COMPLETED,
                        "Auction Completed — " + savedConfig.getSeasonName(),
                        "The auction has concluded. Check your team roster.", NotificationPriority.NORMAL);
            }
        }
        return savedConfig;
    }

    // ── GET CURRENT PLAYER (with bid info) ────────────────────────
    @Override
    @Transactional(readOnly = true)
    public PlayerWithBidResponse getCurrentPlayer(Long configId) {
        AuctionConfig config = configRepo.findById(configId).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionConfig", configId));

        // First check if a player is already in SELLING state
        java.util.Optional<AuctionPlayer> sellingPlayerOpt = playerRepo.findSellingPlayer(configId);
        
        AuctionPlayer player;
        if (sellingPlayerOpt.isPresent()) {
            player = sellingPlayerOpt.get();
        } else {
            List<AuctionPlayer> queued = playerRepo.findQueuedByConfig(configId);
            if (queued.isEmpty()) {
                throw new AuctionStateException("No more players in queue");
            }
            player = queued.get(0);
        }

        Long currentBid = bidRepo.findMaxBidForPlayer(player.getId())
            .orElse((long) player.getBasePrice());

        String currentBidTeam = bidRepo.findTopBidsForPlayer(player.getId(), PageRequest.of(0, 1))
            .stream().findFirst()
            .map(b -> b.getTeam().getTeamName())
            .orElse(null);

        int nextIncrement = config.calculateNextIncrement(currentBid);
        long nextBid = currentBid + nextIncrement;

        return new PlayerWithBidResponse(
            player.getId(), player.getPlayerName(), player.getCategory(),
            player.getPlayerRole(), player.getAge(), player.getBasePrice(),
            player.getStatsJson(), currentBid, nextBid, nextIncrement,
            currentBidTeam, player.getQueueOrder(), player.getStatus().name()
        );
    }

    // ── PLACE BID ─────────────────────────────────────────────────
    @Override
    @Transactional
    public AuctionBid placeBid(BidRequest req, Long biddingUserId) {
        AuctionConfig config = configRepo.findById(req.configId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionConfig", req.configId()));

        // Validate auction is live
        if (config.getStatus() !=  AuctionConfig.AuctionStatus.LIVE  &&
                config.getStatus() != AuctionConfig.AuctionStatus.ACTIVE)
            throw new AuctionStateException("Auction is not LIVE");

        // Lock player row to serialize all bids on the same player
        AuctionPlayer player = playerRepo.findByIdForUpdate(req.playerId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionPlayer", req.playerId()));

        // Lock team row to get a consistent budget snapshot
        AuctionTeam team = teamRepo.findByIdForUpdate(req.teamId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionTeam", req.teamId()));

        // Budget check (under lock — no concurrent bid can see stale budget)
        if (team.getRemainingBudget() < req.bidAmount())
            throw new AuctionStateException("Team budget insufficient. Available: ₹"
                + team.getRemainingBudget());

        // Minimum bid check (under player lock — serialized per player)
        Long currentMax = bidRepo.findMaxBidForPlayer(player.getId()).orElse(0L);
        long minRequired = currentMax == 0
            ? config.getBasePrice()
            : currentMax + config.calculateNextIncrement(currentMax);

        if (req.bidAmount() < minRequired)
            throw new AuctionStateException("Bid must be at least ₹" + minRequired
                + ". Increment rule: "
                + (currentMax >= config.getBidIncrementThreshold() ? "₹" + config.getBidIncrementAbove() : "₹" + config.getBidIncrementDefault()));

        // Set player to SELLING if still QUEUED
        if (player.getStatus() == AuctionPlayer.PlayerStatus.QUEUED) {
            player.setStatus(AuctionPlayer.PlayerStatus.SELLING);
            playerRepo.save(player);
        }

        long baseline = currentMax == 0 ? config.getBasePrice() : currentMax;
        int incrementUsed = (int) (req.bidAmount() - baseline);

        AuctionBid bid = AuctionBid.builder()
            .config(config)
            .player(player)
            .team(team)
            .bidAmount(req.bidAmount())
            .incrementUsed(incrementUsed)
            .isRtm(Boolean.TRUE.equals(req.isRtm()))
            .bidByUser(userRepo.getReferenceById(biddingUserId))
            .build();

        // Capture previous leading team before saving new bid (for outbid notification)
        AuctionTeam previousLeader = bidRepo.findTopBidsForPlayer(player.getId(), PageRequest.of(0, 1))
                .stream().findFirst().map(AuctionBid::getTeam).orElse(null);

        log.info("Bid placed: player={} team={} amount={}", player.getPlayerName(),
            team.getTeamName(), req.bidAmount());
        AuctionBid saved = bidRepo.save(bid);
        // Metric: count every accepted bid (after all validation passed). Tagged with
        // RTM vs normal so the live-auction bid rate is visible in Grafana / alarms.
        meterRegistry.counter("auction.bids.placed",
            "rtm", String.valueOf(Boolean.TRUE.equals(req.isRtm()))).increment();
        auditService.record(
            com.manacommunity.sports.security.AuditAction.BID_PLACED,
            com.manacommunity.sports.security.AuditModule.AUCTION,
            "AuctionPlayer", String.valueOf(player.getId()),
            null,
            "team=" + team.getTeamName() + ", amount=" + req.bidAmount());

        try {
            if (previousLeader != null && previousLeader.getOwnerUser() != null
                    && !previousLeader.getId().equals(team.getId())) {
                notificationService.createNotification(
                        previousLeader.getOwnerUser().getId(), NotificationType.BID_OUTBID, NotificationCategory.AUCTION,
                        "Outbid on " + player.getPlayerName(),
                        team.getTeamName() + " bid ₹" + req.bidAmount(),
                        null, ReferenceType.AUCTION_PLAYER, player.getId(),
                        NotificationPriority.HIGH, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist outbid notification: {}", e.getMessage());
        }


        auctionWs.broadcastBid(config.getId(), AuctionBidResponse.builder()
            .id(saved.getId()).configId(config.getId())
            .playerId(player.getId()).teamId(team.getId())
            .teamName(team.getTeamName()).bidAmount(saved.getBidAmount())
            .incrementUsed(saved.getIncrementUsed()).isRtm(saved.getIsRtm())
            .bidByUserId(biddingUserId).bidAt(saved.getBidAt()).build());

        return saved;
    }

    // ── SOLD PLAYER ───────────────────────────────────────────────
    @Override
    @Transactional
    public AuctionPlayer soldPlayer(SoldPlayerRequest req, Long adminUserId) {
        // Lock player first, then team — consistent ordering prevents deadlocks
        AuctionPlayer player = playerRepo.findByIdForUpdate(req.playerId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionPlayer", req.playerId()));
        AuctionTeam   team   = teamRepo.findByIdForUpdate(req.teamId()).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionTeam", req.teamId()));

        if (player.getStatus() == AuctionPlayer.PlayerStatus.SOLD)
            throw new AuctionStateException("Player " + player.getPlayerName() + " is already SOLD");

        Long soldPrice = bidRepo.findMaxBidForPlayer(player.getId())
            .orElseThrow(() -> new IllegalStateException("No bids placed for this player"));

        // Atomic budget deduction under pessimistic lock
        if (team.getRemainingBudget() < soldPrice)
            throw new AuctionStateException("Team budget insufficient for final sale");

        team.setRemainingBudget(team.getRemainingBudget() - soldPrice);
        team.setSpent((team.getSpent() == null ? 0L : team.getSpent()) + soldPrice);
        teamRepo.save(team);

        player.setStatus(AuctionPlayer.PlayerStatus.SOLD);
        player.setAssignedTeam(team);
        player.setSoldPrice(soldPrice);
        player.setRtmUsed(false);
        player.setSoldAt(LocalDateTime.now());

        // Log it
        logRepo.save(AuctionSessionLog.builder()
            .config(player.getConfig())
            .action("PLAYER_SOLD")
            .player(player)
            .team(team)
            .amount(soldPrice)
            .performedBy(userRepo.getReferenceById(adminUserId))
            .notes(player.getPlayerName() + " sold to " + team.getTeamName() + " for ₹" + soldPrice)
            .build());

        log.info("SOLD: {} → {} for ₹{}", player.getPlayerName(), team.getTeamName(), soldPrice);
        AuctionPlayer savedPlayer = playerRepo.save(player);
        auditService.record(
            com.manacommunity.sports.security.AuditAction.PLAYER_SOLD,
            com.manacommunity.sports.security.AuditModule.AUCTION,
            "AuctionPlayer", String.valueOf(player.getId()),
            null,
            "soldTo=" + team.getTeamName() + ", price=" + soldPrice);

        try {
            if (team.getOwnerUser() != null && team.getOwnerUser().getId() != null) {
                notificationService.createNotification(
                        team.getOwnerUser().getId(), NotificationType.PLAYER_SOLD, NotificationCategory.AUCTION,
                        player.getPlayerName() + " sold to " + team.getTeamName(),
                        "Acquired for ₹" + soldPrice + " | Budget remaining: ₹" + team.getRemainingBudget(),
                        null, ReferenceType.AUCTION_PLAYER, player.getId(),
                        NotificationPriority.HIGH, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist player-sold notification: {}", e.getMessage());
        }


        auctionWs.broadcastPlayerSold(player.getConfig().getId(),
            new AuctionWebSocketService.PlayerSoldPayload(
                player.getId(), player.getPlayerName(),
                team.getId(), team.getTeamName(), soldPrice));

        return savedPlayer;
    }

    // ── PASS PLAYER ───────────────────────────────────────────────
    @Override
    @Transactional
    public AuctionPlayer passPlayer(Long playerId, Long adminUserId) {
        AuctionPlayer player = playerRepo.findByIdForUpdate(playerId).orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionPlayer", playerId));
        AuctionConfig config = player.getConfig();

        if (config.getUnsoldRule() == AuctionConfig.UnsoldRule.ROTATION_AUCTION) {
            // Rotation: move player to end of queue
            int maxOrder = playerRepo.findQueuedByConfig(config.getId())
                .stream().mapToInt(AuctionPlayer::getQueueOrder).max().orElse(0);
            player.setQueueOrder(maxOrder + 1);
            player.setStatus(AuctionPlayer.PlayerStatus.QUEUED);
        } else {
            player.setStatus(AuctionPlayer.PlayerStatus.PASSED);
        }

        logRepo.save(AuctionSessionLog.builder()
            .config(config).action("PLAYER_PASSED").player(player)
            .performedBy(userRepo.getReferenceById(adminUserId)).build());

        AuctionPlayer saved = playerRepo.save(player);

        auctionWs.broadcastPlayerPassed(config.getId(),
            new AuctionWebSocketService.PlayerPassedPayload(
                player.getId(), player.getPlayerName(),
                player.getStatus().name(), player.getQueueOrder()));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionStatsResponse getAuctionStats(Long configId) {
        AuctionConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionConfig", configId));

        long totalPlayers = 0;
        if (config.getEvent() != null) {
            // Direct reference to the linked event is now available
            totalPlayers = registrationRepo.countByEventIdAndStatus(config.getEvent().getId(), com.manacommunity.sports.model.SportsEventRegistration.RegistrationStatus.CONFIRMED);
        } else {
            // Fallback if no event linked
            Long communityId = config.getCreatedBy() != null && config.getCreatedBy().getCommunity() != null 
                    ? config.getCreatedBy().getCommunity().getId() : null;
            Long sportId = config.getSport().getId();
            if (communityId != null) {
                totalPlayers = registrationRepo.countActiveRegistrationsForCommunityAndSport(
                        communityId, 
                        sportId, 
                        //com.manacommunity.sports.model.SportsEventRegistration.RegistrationStatus.CONFIRMED,
                        java.util.Arrays.asList(com.manacommunity.sports.model.SportsEvent.EventStatus.COMPLETED, com.manacommunity.sports.model.SportsEvent.EventStatus.CANCELLED)
                );
            } else {
                totalPlayers = playerRepo.countByConfigId(configId);
            }
        }

        // Live stats directly from database using specific count/sum queries
        long soldPlayers = playerRepo.countByConfigIdAndStatus(configId, AuctionPlayer.PlayerStatus.SOLD);
        long queuedPlayers = playerRepo.countQueuedByConfig(configId);
        
        long totalTeams = teamRepo.countByConfigId(configId);
        long totalBudget = teamRepo.sumBudgetByConfigId(configId);
        long totalSpent = teamRepo.sumSpentByConfigId(configId);

        return new AuctionStatsResponse(totalPlayers, soldPlayers, queuedPlayers, totalTeams, totalBudget, totalSpent);
    }

    @Override
    @Transactional(readOnly = true)
    public long getConfirmedRegistrationCount(Long configId) {
        AuctionConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("AuctionConfig", configId));

        if (config.getEvent() != null) {
            return registrationRepo.countByEventIdAndStatus(config.getEvent().getId(), com.manacommunity.sports.model.SportsEventRegistration.RegistrationStatus.CONFIRMED);
        }

        // Fallback
        Long communityId = config.getCreatedBy() != null && config.getCreatedBy().getCommunity() != null 
                ? config.getCreatedBy().getCommunity().getId() : null;
        Long sportId = config.getSport().getId();
        if (communityId != null) {
            return registrationRepo.countActiveRegistrationsForCommunityAndSport(
                    communityId, 
                    sportId, 
                    //com.manacommunity.sports.model.SportsEventRegistration.RegistrationStatus.CONFIRMED,
                    java.util.Arrays.asList(com.manacommunity.sports.model.SportsEvent.EventStatus.COMPLETED, com.manacommunity.sports.model.SportsEvent.EventStatus.CANCELLED)
            );
        }
        return 0;
    }

    // ── GET BID HISTORY ───────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<AuctionBid> getBidHistory(Long playerId) {
        return bidRepo.findByPlayerIdOrderByBidAtDesc(playerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionPlayer> getPlayers(Long configId, String category, String status) {
        if (category != null) {
            return playerRepo.findByConfigIdAndCategoryOrderByQueueOrder(configId, category);
        }
        if (status != null) {
             return playerRepo.findByConfigIdAndStatusOrderByQueueOrderAsc(configId, AuctionPlayer.PlayerStatus.valueOf(status));
        }
        return playerRepo.findByConfigId(configId);
    }

    @Override
    @Transactional
    public AuctionPlayer createPlayer(Long configId, AuctionPlayerRequest req) {
        AuctionConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid auction config ID"));

        // Compile stats JSON string manually or using a library.
        // For simplicity, constructing a JSON string from the DTO properties.
        StringBuilder statsJson = new StringBuilder("{");
        if (req.getMatches() != null) statsJson.append("\"matches\":").append(req.getMatches()).append(",");
        if (req.getRuns() != null) statsJson.append("\"runs\":").append(req.getRuns()).append(",");
        if (req.getWickets() != null) statsJson.append("\"wickets\":").append(req.getWickets()).append(",");
        if (req.getStrikeRate() != null) statsJson.append("\"strikeRate\":").append(req.getStrikeRate()).append(",");
        if (req.getEconomy() != null) statsJson.append("\"economy\":").append(req.getEconomy()).append(",");
        if (req.getAvgScore() != null) statsJson.append("\"avgScore\":").append(req.getAvgScore()).append(",");
        
        String finalStats = "{}";
        if (statsJson.length() > 1) {
            statsJson.deleteCharAt(statsJson.length() - 1); // remove trailing comma
            statsJson.append("}");
            finalStats = statsJson.toString();
        }

        // Get max queue order for the config to append this player
        int maxOrder = playerRepo.findByConfigIdAndCategoryOrderByQueueOrder(configId, req.getCategory())
                                 .stream().mapToInt(AuctionPlayer::getQueueOrder).max().orElse(0);

        AuctionPlayer player = AuctionPlayer.builder()
            .config(config)
            .playerName(req.getPlayerName())
            .category(req.getCategory())
            .playerRole(req.getPlayerRole())
            .age(req.getAge())
            .basePrice(req.getBasePrice())
            .queueOrder(maxOrder + 1)
            .status(AuctionPlayer.PlayerStatus.QUEUED)
            .statsJson(finalStats)
            .build();

        return playerRepo.save(player);
    }

    private void notifyAuctionTeamOwners(AuctionConfig config, NotificationType type,
                                           String title, String body, NotificationPriority priority) {
        try {
            List<Long> ownerIds = teamRepo.findByConfigIdOrderByTeamName(config.getId()).stream()
                    .filter(t -> t.getOwnerUser() != null && t.getOwnerUser().getId() != null)
                    .map(t -> t.getOwnerUser().getId())
                    .distinct()
                    .toList();
            if (!ownerIds.isEmpty()) {
                notificationService.createBulkNotifications(
                        ownerIds, type, NotificationCategory.AUCTION,
                        title, body, null,
                        ReferenceType.AUCTION_CONFIG, config.getId(),
                        priority, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist {} notifications for auction {}: {}", type, config.getId(), e.getMessage());
        }
    }

    // ── PICK RANDOM PLAYER FOR LIVE AUCTION ──────────────────────
    @Override
    @Transactional
    public PlayerWithBidResponse pickRandomPlayer(Long configId) {
        // Lock the config row to serialize all pick operations for this auction
        AuctionConfig config = configRepo.findByIdForUpdate(configId)
            .orElseThrow(() -> new IllegalArgumentException("Auction config not found: " + configId));

        // Start the auction automatically if it's currently ACTIVE
        if (config.getStatus() == AuctionConfig.AuctionStatus.ACTIVE) {
            config.setStatus(AuctionConfig.AuctionStatus.LIVE);
            configRepo.save(config);
        }

        // Check if there's already a SELLING player — return them first
        var sellingOpt = playerRepo.findSellingPlayer(configId);
        if (sellingOpt.isPresent()) {
            AuctionPlayer selling = sellingOpt.get();
            Long currentBid = bidRepo.findMaxBidForPlayer(selling.getId())
                .orElse((long) selling.getBasePrice());
            String currentBidTeam = bidRepo.findTopBidsForPlayer(selling.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 1))
                .stream().findFirst()
                .map(b -> b.getTeam().getTeamName())
                .orElse(null);
            int nextIncrement = config.calculateNextIncrement(currentBid);
            return new PlayerWithBidResponse(
                selling.getId(), selling.getPlayerName(), selling.getCategory(),
                selling.getPlayerRole(), selling.getAge(), selling.getBasePrice(),
                selling.getStatsJson(), currentBid, currentBid + nextIncrement,
                nextIncrement, currentBidTeam, selling.getQueueOrder(),
                selling.getStatus().name()
            );
        }

        // Pick a random QUEUED player
        List<AuctionPlayer> queued = playerRepo.findQueuedByConfig(configId);
        if (queued.isEmpty()) {
            throw new AuctionStateException("No more players in queue. Auction pool is empty.");
        }

        java.util.Random random = new java.util.Random();
        int randomIndex = random.nextInt(queued.size());
        AuctionPlayer picked = queued.get(randomIndex);

        // Lock the chosen player row before mutating status
        picked = playerRepo.findByIdForUpdate(picked.getId())
            .orElseThrow(() -> new IllegalStateException("Player disappeared during pick"));
        if (picked.getStatus() != AuctionPlayer.PlayerStatus.QUEUED) {
            throw new AuctionStateException("Player " + picked.getPlayerName() + " is no longer QUEUED");
        }

        picked.setStatus(AuctionPlayer.PlayerStatus.SELLING);
        playerRepo.save(picked);

        log.info("Random player picked for auction: {} (id={})", picked.getPlayerName(), picked.getId());

        PlayerWithBidResponse response = new PlayerWithBidResponse(
            picked.getId(), picked.getPlayerName(), picked.getCategory(),
            picked.getPlayerRole(), picked.getAge(), picked.getBasePrice(),
            picked.getStatsJson(), (long) picked.getBasePrice(),
            config.calculateNextBid(picked.getBasePrice()),
            config.calculateNextIncrement(picked.getBasePrice()),
            null, picked.getQueueOrder(), picked.getStatus().name()
        );

        auctionWs.broadcastPlayerPicked(configId, response);

        return response;
    }
}
