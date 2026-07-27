package com.manacommunity.sports.service.scheduler;

import com.manacommunity.sports.dto.scheduler.*;
import com.manacommunity.sports.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.sports.model.scheduler.*;
import com.manacommunity.sports.repository.*;
import com.manacommunity.sports.repository.scheduler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Orchestrator/facade for tournament scheduling. Holds the focused collaborators
 * (validation, seeding, bracket generation, court/time allocation, persistence,
 * notifications) and exposes the public API the controller depends on, delegating
 * the work. Response mapping and simple reads live here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentSchedulerService {

    // ── Collaborators (the segregated services) ───────────────────────
    private final RegistrationValidator       registrationValidator;
    private final SeedingService              seedingService;
    private final BracketGenerator            bracketGenerator;
    private final MatchPersistenceService     matchPersistence;
    private final SchedulerNotificationService notificationService;
    private final TournamentResultNotifier     resultNotifier;

    // ── Repositories used for read/response mapping only ──────────────
    private final TournamentConfigRepository  configRepo;
    private final TournamentGroupRepository   groupRepo;
    private final TournamentMatchRepository   matchRepo;
    private final GroupTeamStandingRepository standingRepo;
    private final VenueRepository             venueRepo;

    // ═══════════════════════════════════════════════════════════════
    // ENTRY POINT — builds config + schedule from request
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public TournamentScheduleResponse createTournamentSchedule(
            TournamentConfigRequest req, Long adminUserId) {

        // Validate teams
        List<AuctionTeam> teams = registrationValidator.validateTeams(req);

        // Persist config (DRAFT)
        TournamentConfig config = matchPersistence.saveTournamentConfig(req, adminUserId);
        log.info("Tournament config saved: [{}] type={}", config.getTournamentName(), config.getTournamentType());

        // Seed if required
        if (Boolean.TRUE.equals(req.hasSeeding())) {
            teams = seedingService.seed(teams);
        }

        // Generate + persist schedule
        List<TournamentMatch> matches = bracketGenerator.generate(config, teams);
        matchRepo.saveAll(matches);
        log.info("Generated {} matches for tournament [{}]", matches.size(), config.getTournamentName());
        return buildResponse(config);
    }

    public TournamentScheduleResponse getSchedule(Long configId) {
        TournamentConfig config = configRepo.findById(configId)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentConfig", configId));
        return buildResponse(config);
    }

    public TournamentMatch rescheduleMatch(Long matchId, String scheduledAtStr, String venue) {
        TournamentMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", matchId));
        LocalDateTime scheduledAt = LocalDateTime.parse(scheduledAtStr);
        match.setScheduledAt(scheduledAt);
        if (venue != null) {
            try {
                Long venueId = Long.parseLong(venue);
                match.setVenue(venueRepo.findById(venueId).orElseThrow(() -> new ResourceNotFoundException("Venue", venueId)));
            } catch (NumberFormatException e) {
                // legacy string venue — ignore
            }
        }
        return matchRepo.save(match);
    }

    public List<MatchResponse> getUpcomingMatchesForTeam(Long teamId) {
        // Placeholder — returns empty list (no custom query yet).
        return List.of();
    }

    // ═══════════════════════════════════════════════════════════════
    // BRACKET advance / seeding / swiss (delegate to BracketGenerator)
    // ═══════════════════════════════════════════════════════════════
    @Transactional
    public TournamentScheduleResponse advanceBracket(MatchResultRequest req) {
        TournamentConfig config = bracketGenerator.applyResult(req);
        // Winner notification (every result) + tournament-completion email (on the FINAL).
        resultNotifier.notifyMatchResult(req.matchId());
        return buildResponse(config);
    }

    public void seedKnockoutFromGroups(Long configId) {
        bracketGenerator.seedKnockoutFromGroups(configId);
    }

    public List<TournamentMatch> generateNextSwissRound(Long configId) {
        return bracketGenerator.generateNextSwissRound(configId);
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFIG CRUD / MANUAL / PERSISTENCE (delegate to MatchPersistenceService)
    // ═══════════════════════════════════════════════════════════════
    public TournamentConfig saveTournamentConfig(TournamentConfigRequest req, Long adminUserId) {
        return matchPersistence.saveTournamentConfig(req, adminUserId);
    }

    public TournamentConfig updateTournamentConfig(Long configId, TournamentConfigRequest req) {
        return matchPersistence.updateTournamentConfig(configId, req);
    }

    public TournamentConfigResponse toConfigResponse(TournamentConfig c) {
        return matchPersistence.toConfigResponse(c);
    }

    public List<TournamentConfigResponse> getConfigsByCommunity(Long communityId) {
        return matchPersistence.getConfigsByCommunity(communityId);
    }

    public void assignTeamsToGroups(Long configId, List<GroupAssignmentRequest> assignments) {
        matchPersistence.assignTeamsToGroups(configId, assignments);
    }

    public void scheduleManualMatch(Long configId, MatchScheduleRequest req) {
        matchPersistence.scheduleManualMatch(configId, req);
    }

    /**
     * Unified deferred save. Persistence runs in its own transaction; on PUBLISHED
     * we notify participants AFTER it commits so a rollback never sends notifications.
     */
    public ScheduleSaveResponse saveSchedule(ScheduleSaveRequest req, Long adminUserId) {
        ScheduleSaveResponse resp = matchPersistence.saveSchedule(req, adminUserId);
        if (isPublished(req.status()) && resp.config() != null) {
            notificationService.notifySchedulePublished(resp.config().id());
        }
        return resp;
    }

    public int saveMatchesBulk(Long configId, List<BulkMatchSaveRequest.MatchData> matches) {
        return matchPersistence.saveMatchesBulk(configId, matches);
    }

    public int updateMatchesStatus(Long configId, String statusStr) {
        int updated = matchPersistence.updateMatchesStatus(configId, statusStr);
        if (isPublished(statusStr)) {
            notificationService.notifySchedulePublished(configId);
        }
        return updated;
    }

    public int deleteMatchesByConfigId(Long configId) {
        return matchPersistence.deleteMatchesByConfigId(configId);
    }

    private boolean isPublished(String status) {
        return status != null && "PUBLISHED".equalsIgnoreCase(status.trim());
    }

    // ═══════════════════════════════════════════════════════════════
    // RESPONSE MAPPING (reads)
    // ═══════════════════════════════════════════════════════════════
    private TournamentScheduleResponse buildResponse(TournamentConfig config) {
        List<TournamentMatch> matches = matchRepo.findByConfigIdOrderByScheduledAt(config.getId());
        List<TournamentGroup> groups  = groupRepo.findByConfigIdOrderByGroupOrder(config.getId());

        int totalRounds = matches.stream()
            .mapToInt(TournamentMatch::getRoundNumber).max().orElse(0);

        // Build group responses
        List<GroupResponse> groupResponses = groups.stream().map(g -> {
            List<StandingResponse> standings = standingRepo
                .findByGroupIdOrderByPointsDescNetRunRateDesc(g.getId())
                .stream().map(this::toStandingResponse)
                .collect(Collectors.toList());
            List<MatchResponse> gMatches = matches.stream()
                .filter(m -> g.equals(m.getGroup()))
                .map(this::toMatchResponse).toList();
            return new GroupResponse(g.getId(), g.getGroupName(), standings, gMatches);
        }).toList();

        // Build round responses
        Map<Integer, List<TournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(TournamentMatch::getRoundNumber));
        List<RoundResponse> rounds = byRound.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new RoundResponse(
                e.getValue().get(0).getRound().name(),
                e.getKey(),
                e.getValue().stream().map(this::toMatchResponse).toList()))
            .toList();

        return new TournamentScheduleResponse(
            config.getId(), config.getTournamentName(), config.getTournamentType().name(),
            config.getTotalTeams(), matches.size(), totalRounds,
            config.getStartDate(), config.getEndDate(),
            groupResponses, rounds,
            matches.stream().map(this::toMatchResponse).toList()
        );
    }

    public MatchResponse toMatchResponse(TournamentMatch m) {
        return new MatchResponse(
            m.getId(), m.getRound() != null ? m.getRound().name() : "TBD",
            Objects.requireNonNullElse(m.getMatchNumber(), 0),
            Objects.requireNonNullElse(m.getBracketSlot(), 0),
            m.getTeamA() != null ? m.getTeamA().getId() : null,
            m.getTeamA() != null ? m.getTeamA().getTeamName() : "TBD",
            m.getTeamB() != null ? m.getTeamB().getId() : null,
            m.getTeamB() != null ? m.getTeamB().getTeamName() : "TBD",
            m.getTeamA() != null ? m.getTeamA().getColorHex() : "#555",
            m.getTeamB() != null ? m.getTeamB().getColorHex() : "#555",
            m.getScheduledAt() != null ? m.getScheduledAt().toString() : "",
            m.getVenue() != null ? m.getVenue().getId() : null,
            m.getVenue() != null ? m.getVenue().getName() : null,
            m.getCourt() != null ? m.getCourt().getId() : null,
            m.getCourt() != null ? m.getCourt().getName() : null,
            m.getStatus().name(),
            m.getScoreTeamA(), m.getScoreTeamB(),
            m.getWinner() != null ? m.getWinner().getTeamName() : null,
            m.getWinnerAdvancesToMatchId(),
            m.getStatus() == MatchStatus.BYE
        );
    }

    private StandingResponse toStandingResponse(GroupTeamStanding s) {
        return new StandingResponse(
            0, s.getTeam().getId(), s.getTeam().getTeamName(),
            s.getTeam().getColorHex(),
            s.getPlayed(), s.getWon(), s.getLost(), s.getDrawn(), s.getPoints(),
            Objects.requireNonNullElse(s.getNetRunRate(), 0.0), s.getQualified()
        );
    }
}
