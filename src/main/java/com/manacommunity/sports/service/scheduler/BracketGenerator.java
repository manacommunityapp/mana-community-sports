package com.manacommunity.sports.service.scheduler;

import com.manacommunity.sports.security.AuditAction;

import com.manacommunity.sports.security.AuditModule;

import com.manacommunity.sports.security.AuditService;

import com.manacommunity.sports.dto.scheduler.MatchResultRequest;
import com.manacommunity.sports.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.sports.model.Court;
import com.manacommunity.sports.model.scheduler.*;
import com.manacommunity.sports.repository.AuctionTeamRepository;
import com.manacommunity.sports.repository.scheduler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates match fixtures for every supported tournament format and advances
 * brackets as results come in. Time slots come from {@link TimeSlotAllocator}
 * and courts are distributed round-robin via {@link CourtAllocator}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BracketGenerator {

    private final TournamentConfigRepository  configRepo;
    private final TournamentGroupRepository   groupRepo;
    private final TournamentMatchRepository   matchRepo;
    private final GroupTeamStandingRepository standingRepo;
    private final AuctionTeamRepository       teamRepo;
    private final SeedingService              seeding;
    private final TimeSlotAllocator           timeSlots;
    private final CourtAllocator              courts;
    private final com.manacommunity.sports.security.AuditService auditService;

    /** Dispatch to the right generator for the config's tournament type. */
    public List<TournamentMatch> generate(TournamentConfig config, List<AuctionTeam> teams) {
        return switch (config.getTournamentType()) {
            case KNOCKOUT, KNOCKOUT_SINGLE, CUSTOM   -> generateKnockout(config, teams);
            case GROUP_KNOCKOUT, GROUP_PLAYOFF        -> generateGroupKnockout(config, teams);
            case ROUND_ROBIN, LEAGUE                  -> generateRoundRobin(config, teams);
            case DOUBLE_ELIMINATION, KNOCKOUT_DOUBLE  -> generateDoubleElimination(config, teams);
            case SWISS                                -> generateSwissRound1(config, teams);
            case SUPER_LEAGUE                         -> generateSuperLeague(config, teams);
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // 1. KNOCKOUT — single elimination bracket
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateKnockout(
            TournamentConfig config, List<AuctionTeam> teams) {

        int n         = teams.size();
        int rounds    = (int) Math.ceil(Math.log(n) / Math.log(2));  // ceil(log2(n))
        int slotCount = (int) Math.pow(2, rounds);                   // nearest power of 2
        int byes      = slotCount - n;

        List<TournamentMatch> matches = new ArrayList<>();
        int matchDur  = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int breakMins = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        TimeSlotAllocator.Cursor slots = timeSlots.cursor(config.getStartDate(), matchDur, breakMins);
        List<Court> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;

        // ── Pad with nulls for byes ─────────────────────────────
        List<AuctionTeam> slotTeams = new ArrayList<>(teams);
        for (int i = 0; i < byes; i++) slotTeams.add(null);  // null = bye

        // ── Round 1 ─────────────────────────────────────────────
        int matchNum = 1;

        for (int i = 0; i < slotCount; i += 2) {
            AuctionTeam teamA = slotTeams.get(i);
            AuctionTeam teamB = slotTeams.get(i + 1);

            boolean isBye          = (teamB == null);
            MatchStatus status     = isBye ? MatchStatus.BYE : MatchStatus.SCHEDULED;
            AuctionTeam autoWinner = isBye ? teamA : null;

            // A bye isn't played, so it does not consume a time slot (the cursor is
            // not advanced for it) — real matches keep consecutive times.
            matches.add(TournamentMatch.builder()
                .config(config)
                .round(rounds == 1 ? MatchRound.FINAL : roundLabel(rounds, 1))
                .roundNumber(1)
                .matchNumber(matchNum++)
                .bracketSlot(i / 2)
                .teamA(teamA)
                .teamB(teamB)
                .scheduledAt(isBye ? slots.peek() : slots.next())
                .durationMinutes(matchDur)
                .venue(config.getVenue())
                .court(courts.pick(venueCourts, courtIdx++))
                .status(status)
                .winner(autoWinner)
                .build());
        }

        // ── Subsequent rounds (placeholder — filled by advanceBracket) ──
        int prevRoundSize = slotCount / 2;
        for (int round = 2; round <= rounds; round++) {
            int roundSize = prevRoundSize / 2;
            MatchRound label = roundLabel(rounds, round);

            for (int i = 0; i < roundSize; i++) {
                matches.add(TournamentMatch.builder()
                    .config(config)
                    .round(label)
                    .roundNumber(round)
                    .matchNumber(i + 1)
                    .bracketSlot(i)
                    .teamA(null)   // TBD — winner of earlier match
                    .teamB(null)
                    .scheduledAt(slots.next())
                    .durationMinutes(matchDur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }

            // Third place match (same slot as final, different schedule)
            if (round == rounds && Boolean.TRUE.equals(config.getThirdPlaceMatch())) {
                matches.add(TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.THIRD_PLACE)
                    .roundNumber(round)
                    .matchNumber(99)
                    .scheduledAt(slots.next())
                    .durationMinutes(matchDur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }
            prevRoundSize = roundSize;
        }

        // ── Wire winnerAdvancesToMatchId links ──────────────────
        wireKnockoutLinks(matches);
        log.info("[KNOCKOUT] {} rounds, {} slots, {} byes, {} matches",
            rounds, slotCount, byes, matches.size());
        return matches;
    }

    /** After a result is entered, advance winner/loser and update standings. Returns the config. */
    @Transactional
    public TournamentConfig applyResult(MatchResultRequest req) {
        TournamentMatch match = matchRepo.findById(req.matchId())
            .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", req.matchId()));

        match.setScoreTeamA(req.scoreTeamA());
        match.setScoreTeamB(req.scoreTeamB());
        match.setStatus(MatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());

        if (req.winnerTeamId() != null) {
            AuctionTeam winner = teamRepo.findById(req.winnerTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", req.winnerTeamId()));
            AuctionTeam loser = winner.getId().equals(match.getTeamA().getId())
                ? match.getTeamB() : match.getTeamA();

            match.setWinner(winner);
            matchRepo.save(match);

            auditService.record(
                com.manacommunity.sports.security.AuditAction.WINNER_DECLARED,
                com.manacommunity.sports.security.AuditModule.TOURNAMENT,
                "TournamentMatch", String.valueOf(match.getId()),
                null,
                "winnerTeamId=" + winner.getId() + ", score=" + req.scoreTeamA() + "-" + req.scoreTeamB());

            // Advance winner to next round match
            if (match.getWinnerAdvancesToMatchId() != null) {
                TournamentMatch next = matchRepo.findById(match.getWinnerAdvancesToMatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", match.getWinnerAdvancesToMatchId()));
                if (next.getTeamA() == null) next.setTeamA(winner);
                else                          next.setTeamB(winner);
                matchRepo.save(next);
            }

            // For double elimination: send loser to losers bracket
            if (match.getLoserSentToMatchId() != null) {
                TournamentMatch losersMatch = matchRepo.findById(match.getLoserSentToMatchId())
                    .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", match.getLoserSentToMatchId()));
                if (losersMatch.getTeamA() == null) losersMatch.setTeamA(loser);
                else                                 losersMatch.setTeamB(loser);
                matchRepo.save(losersMatch);
            }

            // Update group standings for group matches
            if (match.getGroup() != null) {
                updateGroupStandings(match, winner, loser, req);
            }
        } else {
            matchRepo.save(match);
            auditService.record(
                com.manacommunity.sports.security.AuditAction.WINNER_DECLARED,
                com.manacommunity.sports.security.AuditModule.TOURNAMENT,
                "TournamentMatch", String.valueOf(match.getId()),
                null,
                "result=DRAW/TIE, score=" + req.scoreTeamA() + "-" + req.scoreTeamB());

            // Update group standings for draws
            if (match.getGroup() != null) {
                updateGroupStandingsForDraw(match, req);
            }
        }

        return match.getConfig();
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. GROUP_KNOCKOUT — groups + knockout stage
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateGroupKnockout(
            TournamentConfig config, List<AuctionTeam> teams) {

        int nGroups  = Objects.requireNonNullElse(config.getNumberOfGroups(), 2);
        int advPer   = Objects.requireNonNullElse(config.getTeamsAdvancingPerGroup(), 2);
        List<TournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = timeSlots.dayStart(config.getStartDate());
        int dur   = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk   = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // Assign teams to groups (snake-draft for balanced seeding)
        // Team 1 → A, Team 2 → B, Team 3 → B, Team 4 → A …
        List<List<AuctionTeam>> groupTeams = new ArrayList<>();
        for (int g = 0; g < nGroups; g++) groupTeams.add(new ArrayList<>());

        for (int i = 0; i < teams.size(); i++) {
            int row = i / nGroups;
            int col = (row % 2 == 0) ? (i % nGroups) : (nGroups - 1 - (i % nGroups));
            groupTeams.get(col).add(teams.get(i));
        }

        // ── Create group entities ────────────────────────────────
        char letter = 'A';
        for (int g = 0; g < nGroups; g++) {
            TournamentGroup grp = TournamentGroup.builder()
                .config(config)
                .groupName("Group " + (char)(letter + g))
                .groupOrder(g + 1)
                .build();
            grp = groupRepo.save(grp);

            // Standings
            List<AuctionTeam> gt = groupTeams.get(g);
            for (int seed = 0; seed < gt.size(); seed++) {
                standingRepo.save(GroupTeamStanding.builder()
                    .group(grp).team(gt.get(seed)).seedRank(seed + 1)
                    .played(0).won(0).lost(0).drawn(0).points(0).netRunRate(0.0)
                    .build());
            }

            // Group round-robin matches
            List<TournamentMatch> groupMatches =
                roundRobinMatchesForGroup(grp, gt, config, cursor, dur, brk);
            matches.addAll(groupMatches);
            cursor = cursor.plusMinutes((long)(dur + brk) * groupMatches.size());
        }

        // ── Knockout placeholder matches ─────────────────────────
        // Total teams advancing = nGroups * advPer
        int advancing = nGroups * advPer;
        TournamentConfig koConfig = config.toBuilder()
            .thirdPlaceMatch(config.getThirdPlaceMatch())
            .build();

        List<TournamentMatch> koMatches = generateKnockout(koConfig,
            new ArrayList<>(Collections.nCopies(advancing, null))); // TBD teams
        // Adjust cursor for knockout
        for (int i = 0; i < koMatches.size(); i++) {
            koMatches.get(i).setScheduledAt(cursor.plusMinutes((long) (dur + brk) * i));
        }
        matches.addAll(koMatches);

        log.info("[GROUP_KNOCKOUT] {} groups, {} teams advancing per group, {} total matches",
            nGroups, advPer, matches.size());
        return matches;
    }

    /** Called when group stage is complete — seeds winners into knockout */
    @Transactional
    public void seedKnockoutFromGroups(Long configId) {
        TournamentConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("TournamentConfig", configId));
        int advPer = Objects.requireNonNullElse(config.getTeamsAdvancingPerGroup(), 2);

        List<TournamentGroup> groups = groupRepo.findByConfigIdOrderByGroupOrder(configId);
        List<AuctionTeam> advancingTeams = new ArrayList<>();

        // Collect top N from each group by points (then NRR)
        for (TournamentGroup grp : groups) {
            List<GroupTeamStanding> standings =
                standingRepo.findByGroupIdOrderByPointsDescNetRunRateDesc(grp.getId());
            standings.stream().limit(advPer)
                .forEach(s -> {
                    s.setQualified(true);
                    standingRepo.save(s);
                    advancingTeams.add(s.getTeam());
                });
        }

        // Seed into knockout round 1 matches
        // Cross-seeding: Group A #1 vs Group B #2, Group B #1 vs Group A #2
        List<TournamentMatch> koBracket = matchRepo
            .findByConfigIdAndRoundNumberOrderByMatchNumber(configId, 1);

        // Standard cross seeding
        List<AuctionTeam> seeded = seeding.crossSeed(advancingTeams, groups.size(), advPer);
        for (int i = 0; i < koBracket.size() && (i * 2 + 1) < seeded.size(); i++) {
            TournamentMatch m = koBracket.get(i);
            m.setTeamA(seeded.get(i * 2));
            m.setTeamB(seeded.get(i * 2 + 1));
            matchRepo.save(m);
        }
        log.info("Seeded {} advancing teams into knockout bracket", advancingTeams.size());
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. ROUND ROBIN — everyone vs everyone
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateRoundRobin(
            TournamentConfig config, List<AuctionTeam> teams) {

        List<TournamentMatch> matches = new ArrayList<>();
        int n = teams.size();
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        TimeSlotAllocator.Cursor slots = timeSlots.cursor(config.getStartDate(), dur, brk);
        List<Court> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;
        int matchNum = 1;

        // ── Berger round-robin algorithm ────────────────────────
        // Fix team[0], rotate team[1..n-1] for each round
        List<AuctionTeam> pool = new ArrayList<>(teams);
        if (n % 2 != 0) pool.add(null); // null = bye
        int totalSlots = pool.size();
        int rounds = totalSlots - 1;

        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < totalSlots / 2; i++) {
                AuctionTeam teamA = pool.get(i);
                AuctionTeam teamB = pool.get(totalSlots - 1 - i);

                if (teamA == null || teamB == null) continue; // bye round

                matches.add(TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.LEAGUE_MATCH)
                    .roundNumber(round + 1)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(teamA)
                    .teamB(teamB)
                    .scheduledAt(slots.next())
                    .durationMinutes(dur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }

            // Rotate: keep pool[0] fixed, rotate rest
            AuctionTeam last = pool.remove(totalSlots - 1);
            pool.add(1, last);
        }

        log.info("[ROUND_ROBIN] {} teams, {} rounds, {} matches", n, rounds, matches.size());
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4. DOUBLE ELIMINATION — winners + losers bracket
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateDoubleElimination(
            TournamentConfig config, List<AuctionTeam> teams) {

        List<TournamentMatch> matches = new ArrayList<>();
        int n         = teams.size();
        int rounds    = (int) Math.ceil(Math.log(n) / Math.log(2));
        int slotCount = (int) Math.pow(2, rounds);
        List<AuctionTeam> slotTeams = new ArrayList<>(teams);
        while (slotTeams.size() < slotCount) slotTeams.add(null);

        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);
        TimeSlotAllocator.Cursor slots = timeSlots.cursor(config.getStartDate(), dur, brk);
        List<Court> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;
        int matchNum = 1;

        // ── Winners Bracket ─────────────────────────────────────
        for (int round = 1; round <= rounds; round++) {
            int roundSize = slotCount / (int) Math.pow(2, round);
            for (int i = 0; i < roundSize; i++) {
                matches.add(TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.WINNERS_BRACKET)
                    .roundNumber(round)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(round == 1 ? slotTeams.get(i * 2) : null)
                    .teamB(round == 1 ? slotTeams.get(i * 2 + 1) : null)
                    .scheduledAt(slots.next())
                    .durationMinutes(dur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }
        }

        // ── Losers Bracket ──────────────────────────────────────
        int lbRounds = 2 * (rounds - 1);
        for (int round = 1; round <= lbRounds; round++) {
            int lbSize = (int)(slotCount / Math.pow(2, Math.ceil(round / 2.0) + 1));
            lbSize = Math.max(1, lbSize);
            for (int i = 0; i < lbSize; i++) {
                matches.add(TournamentMatch.builder()
                    .config(config)
                    .round(MatchRound.LOSERS_BRACKET)
                    .roundNumber(round)
                    .matchNumber(matchNum++)
                    .bracketSlot(i)
                    .teamA(null).teamB(null)  // filled by advance logic
                    .scheduledAt(slots.next())
                    .durationMinutes(dur)
                    .venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED)
                    .build());
            }
        }

        // ── Grand Final ─────────────────────────────────────────
        matches.add(TournamentMatch.builder()
            .config(config).round(MatchRound.GRAND_FINAL).roundNumber(rounds + lbRounds + 1)
            .matchNumber(matchNum++)
            .scheduledAt(slots.next()).durationMinutes(dur).venue(config.getVenue())
            .court(courts.pick(venueCourts, courtIdx++))
            .status(MatchStatus.SCHEDULED).build());

        log.info("[DOUBLE_ELIM] {} rounds WB, {} rounds LB, {} total matches",
            rounds, lbRounds, matches.size());
        return matches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 5. SWISS SYSTEM — score-paired rounds
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateSwissRound1(
            TournamentConfig config, List<AuctionTeam> teams) {

        int rounds = Objects.requireNonNullElse(
            config.getSwissRounds(),
            (int) Math.ceil(Math.log(teams.size()) / Math.log(2)));

        List<TournamentMatch> matches = new ArrayList<>();
        LocalDateTime cursor = timeSlots.dayStart(config.getStartDate());
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        // Round 1: random pairing
        List<AuctionTeam> shuffled = new ArrayList<>(teams);
        Collections.shuffle(shuffled);
        List<TournamentMatch> round1 = pairSwissRound(config, shuffled, 1, cursor, dur, brk);
        matches.addAll(round1);

        log.info("[SWISS] {} rounds planned, round 1 generated ({} matches)",
            rounds, round1.size());
        return matches;
    }

    /** Called after each Swiss round result to generate next round pairing */
    @Transactional
    public List<TournamentMatch> generateNextSwissRound(Long configId) {
        TournamentConfig config = configRepo.findById(configId)
            .orElseThrow(() -> new ResourceNotFoundException("TournamentConfig", configId));

        int lastRound = matchRepo.findMaxSwissRound(configId).orElse(0);
        int nextRound = lastRound + 1;
        int maxRounds = Objects.requireNonNullElse(config.getSwissRounds(), 5);

        if (nextRound > maxRounds)
            throw new com.manacommunity.sports.exception.AuctionStateException("Swiss tournament already completed " + maxRounds + " rounds");

        // Build score map: teamId → points
        Map<Long, Integer> scores = new HashMap<>();
        matchRepo.findByConfigIdOrderBySwissRoundNumber(configId).stream()
            .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
            .forEach(m -> {
                Long wId = m.getWinner().getId();
                Long lId = wId.equals(m.getTeamA().getId())
                    ? m.getTeamB().getId() : m.getTeamA().getId();
                scores.merge(wId, config.getPointsForWin() != null ? config.getPointsForWin() : 2, Integer::sum);
                scores.merge(lId, config.getPointsForLoss() != null ? config.getPointsForLoss() : 0, Integer::sum);
            });

        Set<Long> teamIdsInConfig = new HashSet<>();
        matchRepo.findByConfigId(configId).forEach(m -> {
            if (m.getTeamA() != null) teamIdsInConfig.add(m.getTeamA().getId());
            if (m.getTeamB() != null) teamIdsInConfig.add(m.getTeamB().getId());
        });
        List<AuctionTeam> tournamentTeams = teamRepo.findAllById(teamIdsInConfig);

        tournamentTeams.sort((a, b) -> {
            int sa = scores.getOrDefault(a.getId(), 0);
            int sb = scores.getOrDefault(b.getId(), 0);
            if (sa != sb) return sb - sa;
            return (int)(Math.random() * 3 - 1); // random tiebreak
        });

        // Build set of already-played pairs to avoid rematches
        Set<String> playedPairs = matchRepo.findByConfigId(configId).stream()
            .filter(m -> m.getTeamA() != null && m.getTeamB() != null)
            .map(m -> Math.min(m.getTeamA().getId(), m.getTeamB().getId()) + "-"
                    + Math.max(m.getTeamA().getId(), m.getTeamB().getId()))
            .collect(Collectors.toSet());

        // Pair adjacent teams by score, avoid rematches
        List<AuctionTeam> paired = swissPairNoRematches(tournamentTeams, playedPairs);
        LocalDateTime cursor = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        int brk = Objects.requireNonNullElse(config.getBreakBetweenMatchesMinutes(), 30);

        List<TournamentMatch> newMatches = pairSwissRound(config, paired, nextRound, cursor, dur, brk);
        matchRepo.saveAll(newMatches);
        log.info("[SWISS] Round {} generated ({} matches)", nextRound, newMatches.size());
        return newMatches;
    }

    // ═══════════════════════════════════════════════════════════════
    // 6. SUPER LEAGUE — IPL-style top-4 playoffs
    // ═══════════════════════════════════════════════════════════════
    private List<TournamentMatch> generateSuperLeague(
            TournamentConfig config, List<AuctionTeam> teams) {

        // Phase 1: Full round-robin league stage (everyone vs everyone)
        List<TournamentMatch> leagueMatches = generateRoundRobin(config, teams);

        // Phase 2: Playoffs for top 4
        int dur = Objects.requireNonNullElse(config.getMatchDurationMinutes(), 90);
        LocalDateTime playoffStart = config.getStartDate()
            .plusDays((long) leagueMatches.size() / 2 + 1).atTime(19, 0);
        List<Court> venueCourts = courts.courtsFor(config);

        List<TournamentMatch> playoffs = List.of(
            buildPlayoffMatch(config, MatchRound.QUALIFIER_1,  1, playoffStart,             dur, venueCourts, 0),
            buildPlayoffMatch(config, MatchRound.ELIMINATOR,   2, playoffStart.plusDays(1), dur, venueCourts, 1),
            buildPlayoffMatch(config, MatchRound.QUALIFIER_2,  3, playoffStart.plusDays(3), dur, venueCourts, 2),
            buildPlayoffMatch(config, MatchRound.SUPER_FINAL,  4, playoffStart.plusDays(5), dur, venueCourts, 3)
        );

        List<TournamentMatch> all = new ArrayList<>(leagueMatches);
        all.addAll(playoffs);
        log.info("[SUPER_LEAGUE] {} league + {} playoff = {} total",
            leagueMatches.size(), playoffs.size(), all.size());
        return all;
    }

    // ═══════════════════════════════════════════════════════════════
    // STANDINGS UPDATE
    // ═══════════════════════════════════════════════════════════════
    private void updateGroupStandings(TournamentMatch match,
            AuctionTeam winner, AuctionTeam loser, MatchResultRequest req) {

        int ptsWin  = Objects.requireNonNullElse(match.getConfig().getPointsForWin(), 2);
        int ptsLoss = Objects.requireNonNullElse(match.getConfig().getPointsForLoss(), 0);

        // Update winner
        GroupTeamStanding ws = standingRepo
            .findByGroupIdAndTeamId(match.getGroup().getId(), winner.getId());
        ws.setPlayed(ws.getPlayed() + 1);
        ws.setWon(ws.getWon() + 1);
        ws.setPoints(ws.getPoints() + ptsWin);
        if (req.runsTeamA() != null) updateNRR(ws, winner, match, req);
        standingRepo.save(ws);

        // Update loser
        GroupTeamStanding ls = standingRepo
            .findByGroupIdAndTeamId(match.getGroup().getId(), loser.getId());
        ls.setPlayed(ls.getPlayed() + 1);
        ls.setLost(ls.getLost() + 1);
        ls.setPoints(ls.getPoints() + ptsLoss);
        if (req.runsTeamB() != null) updateNRR(ls, loser, match, req);
        standingRepo.save(ls);
    }

    private void updateNRR(GroupTeamStanding standing, AuctionTeam team,
            TournamentMatch match, MatchResultRequest req) {
        boolean isTeamA = team.getId().equals(match.getTeamA().getId());
        int runsFor     = isTeamA ? req.runsTeamA() : req.runsTeamB();
        int runsAgainst = isTeamA ? req.runsTeamB() : req.runsTeamA();
        int oversFor    = isTeamA ? req.oversTeamA() : req.oversTeamB();
        int oversAgainst= isTeamA ? req.oversTeamB() : req.oversTeamA();

        standing.setRunsFor(Objects.requireNonNullElse(standing.getRunsFor(), 0) + runsFor);
        standing.setRunsAgainst(Objects.requireNonNullElse(standing.getRunsAgainst(), 0) + runsAgainst);
        standing.setOversFor(Objects.requireNonNullElse(standing.getOversFor(), 0) + oversFor);
        standing.setOversAgainst(Objects.requireNonNullElse(standing.getOversAgainst(), 0) + oversAgainst);

        double nrr = (standing.getOversFor() > 0 ? (double) standing.getRunsFor() / standing.getOversFor() : 0)
                   - (standing.getOversAgainst() > 0 ? (double) standing.getRunsAgainst() / standing.getOversAgainst() : 0);
        standing.setNetRunRate(Math.round(nrr * 1000.0) / 1000.0);
    }

    private void updateGroupStandingsForDraw(TournamentMatch match, MatchResultRequest req) {
        int ptsDraw = Objects.requireNonNullElse(match.getConfig().getPointsForDraw(), 1);

        GroupTeamStanding sa = standingRepo
            .findByGroupIdAndTeamId(match.getGroup().getId(), match.getTeamA().getId());
        sa.setPlayed(sa.getPlayed() + 1);
        sa.setDrawn(sa.getDrawn() + 1);
        sa.setPoints(sa.getPoints() + ptsDraw);
        if (req.runsTeamA() != null) updateNRR(sa, match.getTeamA(), match, req);
        standingRepo.save(sa);

        GroupTeamStanding sb = standingRepo
            .findByGroupIdAndTeamId(match.getGroup().getId(), match.getTeamB().getId());
        sb.setPlayed(sb.getPlayed() + 1);
        sb.setDrawn(sb.getDrawn() + 1);
        sb.setPoints(sb.getPoints() + ptsDraw);
        if (req.runsTeamB() != null) updateNRR(sb, match.getTeamB(), match, req);
        standingRepo.save(sb);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════
    private MatchRound roundLabel(int totalRounds, int round) {
        int fromEnd = totalRounds - round;
        return switch (fromEnd) {
            case 0 -> MatchRound.FINAL;
            case 1 -> MatchRound.SEMI_FINAL;
            case 2 -> MatchRound.QUARTER_FINAL;
            case 3 -> MatchRound.ROUND_OF_16;
            case 4 -> MatchRound.ROUND_OF_32;
            default -> MatchRound.ROUND_OF_64;
        };
    }

    private List<TournamentMatch> roundRobinMatchesForGroup(
            TournamentGroup grp, List<AuctionTeam> teams,
            TournamentConfig config, LocalDateTime start, int dur, int brk) {

        List<TournamentMatch> matches = new ArrayList<>();
        int matchNum = 1;
        TimeSlotAllocator.Cursor slots = timeSlots.cursor(start, dur, brk);
        List<Court> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;

        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                matches.add(TournamentMatch.builder()
                    .config(config).group(grp)
                    .round(MatchRound.GROUP_STAGE).roundNumber(matchNum).matchNumber(matchNum)
                    .teamA(teams.get(i)).teamB(teams.get(j))
                    .scheduledAt(slots.next()).durationMinutes(dur).venue(config.getVenue())
                    .court(courts.pick(venueCourts, courtIdx++))
                    .status(MatchStatus.SCHEDULED).build());
                matchNum++;
            }
        }
        return matches;
    }

    private List<TournamentMatch> pairSwissRound(
            TournamentConfig config, List<AuctionTeam> teams,
            int roundNum, LocalDateTime start, int dur, int brk) {

        List<TournamentMatch> matches = new ArrayList<>();
        TimeSlotAllocator.Cursor slots = timeSlots.cursor(start, dur, brk);
        List<Court> venueCourts = courts.courtsFor(config);
        int courtIdx = 0;
        for (int i = 0; i + 1 < teams.size(); i += 2) {
            matches.add(TournamentMatch.builder()
                .config(config).round(MatchRound.SWISS_ROUND)
                .roundNumber(roundNum).swissRoundNumber(roundNum)
                .matchNumber(i / 2 + 1).bracketSlot(i / 2)
                .teamA(teams.get(i)).teamB(teams.get(i + 1))
                .scheduledAt(slots.next()).durationMinutes(dur).venue(config.getVenue())
                .court(courts.pick(venueCourts, courtIdx++))
                .status(MatchStatus.SCHEDULED).build());
        }
        return matches;
    }

    private List<AuctionTeam> swissPairNoRematches(
            List<AuctionTeam> sorted, Set<String> playedPairs) {

        List<AuctionTeam> result = new ArrayList<>();
        boolean[] used = new boolean[sorted.size()];

        for (int i = 0; i < sorted.size(); i++) {
            if (used[i]) continue;
            for (int j = i + 1; j < sorted.size(); j++) {
                if (used[j]) continue;
                String key = Math.min(sorted.get(i).getId(), sorted.get(j).getId()) + "-"
                           + Math.max(sorted.get(i).getId(), sorted.get(j).getId());
                if (!playedPairs.contains(key)) {
                    result.add(sorted.get(i));
                    result.add(sorted.get(j));
                    used[i] = used[j] = true;
                    break;
                }
            }
        }
        return result;
    }

    private TournamentMatch buildPlayoffMatch(
            TournamentConfig config, MatchRound round,
            int roundNum, LocalDateTime at, int dur,
            List<Court> venueCourts, int courtIndex) {
        return TournamentMatch.builder()
            .config(config).round(round).roundNumber(roundNum).matchNumber(1)
            .scheduledAt(at).durationMinutes(dur).venue(config.getVenue())
            .court(courts.pick(venueCourts, courtIndex))
            .status(MatchStatus.SCHEDULED).build();
    }

    private void wireKnockoutLinks(List<TournamentMatch> matches) {
        // Group by roundNumber
        Map<Integer, List<TournamentMatch>> byRound = matches.stream()
            .collect(Collectors.groupingBy(TournamentMatch::getRoundNumber));

        byRound.forEach((round, rMatches) -> {
            List<TournamentMatch> nextRound = byRound.get(round + 1);
            if (nextRound == null) return;
            for (int i = 0; i < rMatches.size(); i++) {
                TournamentMatch current = rMatches.get(i);
                TournamentMatch next    = nextRound.get(i / 2);
                current.setWinnerFeedFromMatchA(
                    (i % 2 == 0) ? current.getId() : current.getWinnerFeedFromMatchA());
                // Links wired after DB save so IDs are available
            }
        });
    }
}
