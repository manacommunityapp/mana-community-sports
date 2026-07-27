package com.manacommunity.sports.controller.scheduler;

import com.manacommunity.sports.dto.scheduler.*;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.scheduler.TournamentConfig;
import com.manacommunity.sports.model.scheduler.TournamentMatch;
import com.manacommunity.sports.repository.SportsEventRepository;
import com.manacommunity.sports.repository.scheduler.TournamentConfigRepository;
import com.manacommunity.sports.repository.scheduler.TournamentMatchRepository;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.scheduler.TournamentSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.manacommunity.sports.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@RestController
@RequestMapping("/api/tournament")
@RequiredArgsConstructor
public class TournamentSchedulerController {

    private final TournamentSchedulerService schedulerService;
    private final TournamentConfigRepository configRepo;
    private final TournamentMatchRepository  matchRepo;
    private final SportsEventRepository      eventRepo;
    private final LoggedInUserService        loggedInUserService;
    private final com.manacommunity.sports.repository.AuctionConfigRepository auctionConfigRepo;
    private final com.manacommunity.sports.service.scheduler.PlayoffScheduleGenerator playoffGenerator;
    private final com.manacommunity.sports.service.scheduler.MatchResultService matchResultService;

    // ═══════════════════════════════════════════════════════════════
    // CONFIG CRUD — save / update (without schedule generation)
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/config
     * Save a new tournament config in DRAFT status.
     */
    @PostMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TournamentConfigResponse> saveConfig(
            @Valid @RequestBody TournamentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TournamentConfig saved = schedulerService.saveTournamentConfig(req, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(schedulerService.toConfigResponse(saved));
    }

    /**
     * PUT /api/tournament/config/{id}
     * Update an existing tournament config.
     */
    @PutMapping("/config/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TournamentConfigResponse> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody TournamentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TournamentConfig updated = schedulerService.updateTournamentConfig(id, req);
        return ResponseEntity.ok(schedulerService.toConfigResponse(updated));
    }

    /**
     * GET /api/tournament/config/{id}
     * Get a single tournament config by ID.
     */
    @GetMapping("/config/{id}")
    public ResponseEntity<TournamentConfigResponse> getConfig(@PathVariable Long id) {
        TournamentConfig config = configRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.sports.exception.ResourceNotFoundException("TournamentConfig", id));
        return ResponseEntity.ok(schedulerService.toConfigResponse(config));
    }

    /**
     * GET /api/tournament/configs
     * Get all tournament configs for the logged-in user's community.
     */
    @GetMapping("/configs")
    public ResponseEntity<List<TournamentConfigResponse>> getConfigs(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(schedulerService.getConfigsByCommunity(communityId));
    }

    // ═══════════════════════════════════════════════════════════════
    // DROPDOWN DATA — events, types, teams
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/tournament/events
     * Returns sports events for the logged-in user's community
     * to populate the "Event" dropdown in the tournament config form.
     */
    @GetMapping("/events")
    public ResponseEntity<List<Map<String, Object>>> getEventsForDropdown(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<SportsEvent> events;
        if (communityId != null) {
            events = eventRepo.findByCommunityIdOrderByEventDateStartDesc(communityId);
        } else {
            events = Collections.emptyList();
        }

        List<Map<String, Object>> result = events.stream().map(e -> {
            com.manacommunity.sports.model.AuctionConfig ac = auctionConfigRepo.findByEventId(e.getId()).orElse(null);
            Integer totalTeams = ac != null && ac.getTotalTeams() != null ? ac.getTotalTeams() : 0;
            String venueStr = "";
            if (e.getVenue() != null) {
                venueStr = e.getVenue().getName();
                if (e.getVenue().getCity() != null) venueStr += ", " + e.getVenue().getCity();
                if (e.getVenue().getArea() != null) venueStr += ", " + e.getVenue().getArea();
                if (e.getVenue().getAddress() != null) venueStr += " - " + e.getVenue().getAddress();
                if (e.getVenue().getMapLink() != null) venueStr += " (" + e.getVenue().getMapLink() + ")";
            }

            return Map.<String, Object>ofEntries(
                Map.entry("id", e.getId()),
                Map.entry("name", e.getName() != null ? e.getName() : ""),
                Map.entry("sportId", e.getSport() != null ? e.getSport().getId() : 0),
                Map.entry("sportName", e.getSport() != null ? e.getSport().getName() : ""),
                Map.entry("communityId", e.getCommunity() != null ? e.getCommunity().getId() : 0),
                Map.entry("communityName", e.getCommunity() != null ? e.getCommunity().getName() : ""),
                Map.entry("eventDateStart", e.getEventDateStart() != null ? e.getEventDateStart().toString() : ""),
                Map.entry("eventDateEnd", e.getEventDateEnd() != null ? e.getEventDateEnd().toString() : ""),
                Map.entry("status", e.getTournament() != null && e.getTournament().getRegistrationStatus() != null ? e.getTournament().getRegistrationStatus().name() : ""),
                Map.entry("totalTeams", totalTeams),
                Map.entry("venueId", e.getVenue() != null ? e.getVenue().getId() : 0),
                Map.entry("venueName", venueStr)
            );
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/tournament/types
     * Returns all supported tournament types with descriptions.
     */
    @GetMapping("/types")
    public ResponseEntity<List<TournamentTypeInfo>> getTournamentTypes() {
        return ResponseEntity.ok(List.of(
            new TournamentTypeInfo("KNOCKOUT",          "Knockout",
                "Single elimination — lose once and you're out. Fast and decisive.",
                "2-64", "ceil(log2 N) rounds"),
                new TournamentTypeInfo("GROUP_KNOCKOUT",    "Group Stage + Knockout",
                "Teams split into groups (A, B …). Top N advance to knockout bracket.",
                "4-32", "Group rounds + Knockout"),
            new TournamentTypeInfo("ROUND_ROBIN",       "Round Robin (League)",
                "Everyone plays everyone once. Winner by most points (NRR tiebreak).",
                "3-20", "N*(N-1)/2 matches"),
            new TournamentTypeInfo("DOUBLE_ELIMINATION","Double Elimination",
                "Winners bracket + Losers bracket. Need two losses to be eliminated.",
                "4-32", "~2x Knockout rounds"),
            new TournamentTypeInfo("SWISS",             "Swiss System",
                "Players paired by score each round. No rematches. Used in Chess.",
                "4-128", "ceil(log2 N) rounds"),
            new TournamentTypeInfo("SUPER_LEAGUE",      "Super League (IPL-style)",
                "Full round-robin league → top 4 in playoffs (Q1/Eliminator/Q2/Final).",
                "6-10", "League + 4 playoff matches")
        ));
    }

    // ═══════════════════════════════════════════════════════════════
    // SCHEDULE GENERATION & BRACKET
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/schedule
     * Validates config, generates all matches, returns full schedule.
     */
    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<TournamentScheduleResponse> createSchedule(
            @Valid @RequestBody TournamentConfigRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(schedulerService.createTournamentSchedule(req, principal.getId()));
    }

    /**
     * GET /api/tournament/{configId}/schedule
     * Returns full schedule: groups, standings, all matches by round.
     */
    @GetMapping("/{configId}/schedule")
    public ResponseEntity<TournamentScheduleResponse> getSchedule(@PathVariable Long configId) {
        return ResponseEntity.ok(schedulerService.getSchedule(configId));
    }

    /**
     * GET /api/tournament
     * List all tournaments for a community.
     */
    @GetMapping
    public ResponseEntity<List<TournamentConfig>> list(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(user.getRole())) {
            targetCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        }
        if (targetCommunityId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(configRepo.findByCommunityIdOrderByCreatedAtDesc(targetCommunityId));
    }

    /**
     * POST /api/tournament/match/result
     * Records result, updates standings/NRR, advances bracket.
     */
    @PostMapping("/match/result")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public ResponseEntity<TournamentScheduleResponse> recordResult(
            @Valid @RequestBody MatchResultRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(schedulerService.advanceBracket(req));
    }

    /**
     * POST /api/tournament/{configId}/seed-knockout
     * Seeds advancing teams into knockout bracket after group stage.
     */
    @PostMapping("/{configId}/seed-knockout")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> seedKnockout(@PathVariable Long configId) {
        schedulerService.seedKnockoutFromGroups(configId);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/tournament/{configId}/swiss/next-round
     * Generates next Swiss round pairings.
     */
    @PostMapping("/{configId}/swiss/next-round")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<MatchResponse>> nextSwissRound(@PathVariable Long configId) {
        return ResponseEntity.ok(
            schedulerService.generateNextSwissRound(configId)
                .stream().map(schedulerService::toMatchResponse).toList());
    }

    /**
     * PUT /api/tournament/match/{matchId}/reschedule
     */
    @PutMapping("/match/{matchId}/reschedule")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<MatchResponse> rescheduleMatch(
            @PathVariable Long matchId,
            @Valid @RequestBody RescheduleRequest req) {
        TournamentMatch updated = schedulerService.rescheduleMatch(matchId, req.scheduledAt(), req.venue());
        return ResponseEntity.ok(schedulerService.toMatchResponse(updated));
    }

    // ═══════════════════════════════════════════════════════════════
    // MANUAL SCHEDULING ENDPOINTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/{configId}/manual/groups/assign
     */
    @PostMapping("/{configId}/manual/groups/assign")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> assignTeamsToGroups(
            @PathVariable Long configId,
            @Valid @RequestBody List<GroupAssignmentRequest> assignments) {
        schedulerService.assignTeamsToGroups(configId, assignments);
        return ResponseEntity.ok("Group assignments saved successfully.");
    }

    /**
     * POST /api/tournament/{configId}/manual/matches
     */
    @PostMapping("/{configId}/manual/matches")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<String> scheduleManualMatch(
            @PathVariable Long configId,
            @Valid @RequestBody MatchScheduleRequest req) {
        schedulerService.scheduleManualMatch(configId, req);
        return ResponseEntity.ok("Match scheduled successfully for " + req.stage());
    }

    /**
     * GET /api/tournament/{configId}/matches
     * Returns all saved matches for a config.
     */
    @GetMapping("/{configId}/matches")
    public ResponseEntity<List<MatchResponse>> getMatches(@PathVariable Long configId) {
        List<MatchResponse> matches = matchRepo.findByConfigId(configId)
            .stream().map(schedulerService::toMatchResponse).toList();
        return ResponseEntity.ok(matches);
    }

    /**
     * POST /api/tournament/{configId}/matches/bulk
     * Replaces all existing matches for the config with the supplied list.
     * Called by the frontend "Save as Draft" and "Save & Publish" buttons.
     */
    @PostMapping("/{configId}/matches/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> saveMatchesBulk(
            @PathVariable Long configId,
            @Valid @RequestBody BulkMatchSaveRequest request) {
        int saved = schedulerService.saveMatchesBulk(configId, request.matches());
        return ResponseEntity.ok(Map.of("saved", saved, "configId", configId));
    }

    /**
     * PUT /api/tournament/{configId}/matches/status
     * Updates the status of all matches for a config.
     * Called by "Save as Draft" (DRAFT) and "Save & Publish" (PUBLISHED).
     */
    @PutMapping("/{configId}/matches/status")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateMatchesStatus(
            @PathVariable Long configId,
            @Valid @RequestBody MatchStatusUpdateRequest request) {
        int updated = schedulerService.updateMatchesStatus(configId, request.status());
        return ResponseEntity.ok(Map.of("updated", updated, "configId", configId, "status", request.status()));
    }

    /**
     * DELETE /api/tournament/{configId}/matches
     * Deletes all matches for a config. Called by the "Clear" button.
     */
    @DeleteMapping("/{configId}/matches")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteMatches(@PathVariable Long configId) {
        int deleted = schedulerService.deleteMatchesByConfigId(configId);
        return ResponseEntity.ok(Map.of("deleted", deleted, "configId", configId));
    }

    /**
     * POST /api/tournament/schedule/save
     * Unified, deferred save: persists the tournament config and all customized
     * matches together in a single transaction. Replaces the separate
     * createConfig + saveMatchesBulk calls for the scheduler save flow.
     */
    @PostMapping("/schedule/save")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ScheduleSaveResponse> saveSchedule(
            @Valid @RequestBody ScheduleSaveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(schedulerService.saveSchedule(req, principal.getId()));
    }

    /**
     * POST /api/tournament/playoff/generate
     * Stateless: generates the playoff ("rounds to final") bracket from the given
     * inputs and returns the draft. No DB writes — the UI persists via matches/bulk.
     */
    @PostMapping("/playoff/generate")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PlayoffMatchDraftResponse>> generatePlayoff(
            @Valid @RequestBody PlayoffGenerateRequest request) {
        return ResponseEntity.ok(playoffGenerator.buildPlayoffBracket(request));
    }

    // ═══════════════════════════════════════════════════════════════
    // DETAILED RESULTS, SCORECARDS & LEADERBOARDS
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/tournament/match/result/detail
     * Records detailed result with scorecard (innings, batting, bowling).
     */
    @PostMapping("/match/result/detail")
    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SPORTS_REFEREE','SUPER_ADMIN')")
    public ResponseEntity<MatchDetailResponse> recordDetailedResult(
            @Valid @RequestBody MatchResultDetailRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(matchResultService.recordDetailedResult(req));
    }

    /**
     * GET /api/tournament/match/{matchId}/detail
     * Returns full match detail including scorecard and leaderboard snippet.
     */
    @GetMapping("/match/{matchId}/detail")
    public ResponseEntity<MatchDetailResponse> getMatchDetail(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchResultService.getMatchDetail(matchId));
    }

    /**
     * GET /api/tournament/{configId}/leaderboard?type=runs|wickets|mvp
     * Returns tournament leaderboard.
     */
    @GetMapping("/{configId}/leaderboard")
    public ResponseEntity<List<MatchDetailResponse.PlayerStatsRow>> getLeaderboard(
            @PathVariable Long configId,
            @RequestParam(defaultValue = "runs") String type) {
        return ResponseEntity.ok(matchResultService.getLeaderboard(configId, type));
    }

    /**
     * GET /api/tournament/player/{playerId}/stats
     * Returns player career stats across all tournaments.
     */
    @GetMapping("/player/{playerId}/stats")
    public ResponseEntity<List<MatchDetailResponse.PlayerStatsRow>> getPlayerStats(
            @PathVariable Long playerId) {
        return ResponseEntity.ok(matchResultService.getPlayerCareerStats(playerId));
    }

    record TournamentTypeInfo(
        String id, String name, String description,
        String teamRange, String formatNote) {}
}
