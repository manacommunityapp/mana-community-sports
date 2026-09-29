package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.scheduler.MatchDetailResponse;
import com.manacommunity.sports.dto.scheduler.MatchDetailResponse.*;
import com.manacommunity.sports.dto.scheduler.MatchResultDetailRequest;
import com.manacommunity.sports.dto.scheduler.MatchResultRequest;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.sports.model.scheduler.*;
import com.manacommunity.sports.repository.AuctionPlayerRepository;
import com.manacommunity.sports.repository.AuctionTeamRepository;
import com.manacommunity.sports.repository.scheduler.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchResultService {

    private final TournamentMatchRepository matchRepo;
    private final MatchResultRepository resultRepo;
    private final MatchInningsRepository inningsRepo;
    private final BattingPerformanceRepository battingRepo;
    private final BowlingPerformanceRepository bowlingRepo;
    private final PlayerTournamentStatsRepository statsRepo;
    private final AuctionTeamRepository teamRepo;
    private final AuctionPlayerRepository playerRepo;
    private final BracketGenerator bracketGenerator;
    private final TournamentConfigRepository configRepo;

    @Transactional
    public MatchDetailResponse recordDetailedResult(MatchResultDetailRequest req) {
        TournamentMatch match = matchRepo.findById(req.matchId())
            .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", req.matchId()));

        // 1. Apply basic result via existing bracket advancement logic
        MatchResultRequest basicReq = new MatchResultRequest(
            req.matchId(), req.winnerTeamId(),
            req.scoreTeamA(), req.scoreTeamB(), req.matchNotes(),
            req.runsTeamA(), req.runsTeamB(), req.oversTeamA(), req.oversTeamB()
        );

        if (req.winnerTeamId() != null) {
            bracketGenerator.applyResult(basicReq);
            match = matchRepo.findById(req.matchId())
                    .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", req.matchId()));
        } else {
            match.setScoreTeamA(req.scoreTeamA());
            match.setScoreTeamB(req.scoreTeamB());
            match.setMatchNotes(req.matchNotes());
            match.setStatus(MatchStatus.COMPLETED);
            match.setCompletedAt(java.time.LocalDateTime.now());
            matchRepo.save(match);
        }

        // 2. Set structured result fields on TournamentMatch
        if (req.resultType() != null) {
            match.setResultType(ResultType.valueOf(req.resultType()));
        }
        match.setWinMargin(req.winMargin());
        match.setMatchSummary(req.matchSummary());
        match.setUmpires(req.umpires());

        if (req.tossWinnerTeamId() != null) {
            match.setTossWinner(teamRepo.findById(req.tossWinnerTeamId()).orElse(null));
            match.setTossDecision(req.tossDecision());
        }
        if (req.manOfMatchPlayerId() != null) {
            match.setManOfMatch(playerRepo.findById(req.manOfMatchPlayerId()).orElse(null));
        }
        matchRepo.save(match);

        // 3. Save detailed MatchResult entity
        final TournamentMatch finalMatch = match;
        MatchResult result = resultRepo.findByMatchId(match.getId())
            .orElseGet(() -> MatchResult.builder().match(finalMatch).build());
        result.setResultType(req.resultType() != null ? ResultType.valueOf(req.resultType()) : ResultType.WIN);
        result.setWinMargin(req.winMargin());
        result.setMatchSummary(req.matchSummary());
        result.setUmpires(req.umpires());
        if (req.tossWinnerTeamId() != null)
            result.setTossWinner(teamRepo.findById(req.tossWinnerTeamId()).orElse(null));
        result.setTossDecision(req.tossDecision());
        if (req.manOfMatchPlayerId() != null)
            result.setManOfMatch(playerRepo.findById(req.manOfMatchPlayerId()).orElse(null));
        if (req.bestBatterPlayerId() != null)
            result.setBestBatter(playerRepo.findById(req.bestBatterPlayerId()).orElse(null));
        if (req.bestBowlerPlayerId() != null)
            result.setBestBowler(playerRepo.findById(req.bestBowlerPlayerId()).orElse(null));
        resultRepo.save(result);

        // 4. Save innings + batting/bowling performances
        if (req.innings() != null && !req.innings().isEmpty()) {
            saveInnings(result, req.innings());
        }

        // 5. Recalculate player tournament stats
        recalculatePlayerStats(match.getConfig().getId(), match);

        return getMatchDetail(match.getId());
    }

    private void saveInnings(MatchResult result, List<MatchResultDetailRequest.InningsRequest> inningsReqs) {
        for (var inReq : inningsReqs) {
            AuctionTeam battingTeam = teamRepo.findById(inReq.battingTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", inReq.battingTeamId()));
            AuctionTeam bowlingTeam = teamRepo.findById(inReq.bowlingTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", inReq.bowlingTeamId()));

            BigDecimal overs = parsOvers(inReq.totalOvers());
            BigDecimal runRate = overs.compareTo(BigDecimal.ZERO) > 0
                ? new BigDecimal(inReq.totalRuns()).divide(overs, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            MatchInnings innings = MatchInnings.builder()
                .matchResult(result)
                .inningsNumber(inReq.inningsNumber())
                .battingTeam(battingTeam)
                .bowlingTeam(bowlingTeam)
                .totalRuns(inReq.totalRuns())
                .totalWickets(inReq.totalWickets())
                .totalOvers(overs)
                .extras(inReq.extras())
                .extrasDetail(inReq.extrasDetail())
                .target(inReq.target())
                .runRate(runRate)
                .fallOfWickets(inReq.fallOfWickets())
                .build();
            inningsRepo.save(innings);

            if (inReq.batting() != null) {
                for (var bat : inReq.batting()) {
                    AuctionPlayer player = playerRepo.findById(bat.playerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Player", bat.playerId()));
                    BigDecimal sr = bat.ballsFaced() > 0
                        ? new BigDecimal(bat.runsScored() * 100).divide(new BigDecimal(bat.ballsFaced()), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                    battingRepo.save(BattingPerformance.builder()
                        .innings(innings)
                        .player(player)
                        .battingPosition(bat.battingPosition())
                        .runsScored(bat.runsScored())
                        .ballsFaced(bat.ballsFaced())
                        .fours(bat.fours())
                        .sixes(bat.sixes())
                        .strikeRate(sr)
                        .dismissalType(bat.dismissalType() != null ? DismissalType.valueOf(bat.dismissalType()) : null)
                        .dismissedBy(bat.dismissedByPlayerId() != null ? playerRepo.findById(bat.dismissedByPlayerId()).orElse(null) : null)
                        .fielder(bat.fielderPlayerId() != null ? playerRepo.findById(bat.fielderPlayerId()).orElse(null) : null)
                        .build());
                }
            }

            if (inReq.bowling() != null) {
                for (var bowl : inReq.bowling()) {
                    AuctionPlayer player = playerRepo.findById(bowl.playerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Player", bowl.playerId()));
                    BigDecimal oversB = parsOvers(bowl.oversBowled());
                    BigDecimal eco = oversB.compareTo(BigDecimal.ZERO) > 0
                        ? new BigDecimal(bowl.runsConceded()).divide(oversB, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                    bowlingRepo.save(BowlingPerformance.builder()
                        .innings(innings)
                        .player(player)
                        .bowlingOrder(bowl.bowlingOrder())
                        .oversBowled(oversB)
                        .maidens(bowl.maidens())
                        .runsConceded(bowl.runsConceded())
                        .wicketsTaken(bowl.wicketsTaken())
                        .economyRate(eco)
                        .dotBalls(bowl.dotBalls())
                        .wides(bowl.wides())
                        .noBalls(bowl.noBalls())
                        .build());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public MatchDetailResponse getMatchDetail(Long matchId) {
        TournamentMatch match = matchRepo.findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", matchId));

        MatchResult result = resultRepo.findByMatchId(matchId).orElse(null);

        TeamInfo teamAInfo = match.getTeamA() != null
            ? new TeamInfo(match.getTeamA().getId(), match.getTeamA().getTeamName(), match.getTeamA().getColorHex())
            : new TeamInfo(null, "TBD", "#555");
        TeamInfo teamBInfo = match.getTeamB() != null
            ? new TeamInfo(match.getTeamB().getId(), match.getTeamB().getTeamName(), match.getTeamB().getColorHex())
            : new TeamInfo(null, "TBD", "#555");

        PlayerBrief mom = null, bestBat = null, bestBowl = null;
        String tossWinnerName = null;
        if (result != null) {
            if (result.getManOfMatch() != null) mom = toPlayerBrief(result.getManOfMatch());
            if (result.getBestBatter() != null) bestBat = toPlayerBrief(result.getBestBatter());
            if (result.getBestBowler() != null) bestBowl = toPlayerBrief(result.getBestBowler());
            if (result.getTossWinner() != null) tossWinnerName = result.getTossWinner().getTeamName();
        }

        List<InningsDetail> inningsDetails = new ArrayList<>();
        if (result != null) {
            List<MatchInnings> inningsList = inningsRepo.findByMatchResultIdOrderByInningsNumber(result.getId());
            for (MatchInnings inn : inningsList) {
                List<BattingPerformance> batPerfs = battingRepo.findByInningsIdOrderByBattingPosition(inn.getId());
                List<BowlingPerformance> bowlPerfs = bowlingRepo.findByInningsIdOrderByBowlingOrder(inn.getId());
                inningsDetails.add(toInningsDetail(inn, batPerfs, bowlPerfs));
            }
        }

        Long configId = match.getConfig().getId();
        List<PlayerStatsRow> topRuns = statsRepo.findByConfigIdOrderByTotalRunsDesc(configId)
            .stream().limit(5).map(this::toPlayerStatsRow).toList();
        List<PlayerStatsRow> topWickets = statsRepo.findByConfigIdOrderByTotalWicketsDesc(configId)
            .stream().limit(5).map(this::toPlayerStatsRow).toList();

        return new MatchDetailResponse(
            match.getId(),
            match.getRound() != null ? match.getRound().name() : "TBD",
            match.getMatchNumber() != null ? match.getMatchNumber() : 0,
            match.getScheduledAt() != null ? match.getScheduledAt().toString() : "",
            match.getStatus().name(),
            teamAInfo, teamBInfo,
            match.getResultType() != null ? match.getResultType().name() : null,
            match.getScoreTeamA(), match.getScoreTeamB(),
            match.getWinMargin(),
            match.getWinner() != null ? match.getWinner().getTeamName() : null,
            match.getMatchSummary(), match.getMatchNotes(),
            tossWinnerName,
            result != null ? result.getTossDecision() : null,
            mom, bestBat, bestBowl,
            match.getUmpires(),
            match.getVenue() != null ? match.getVenue().getName() : null,
            match.getCourt() != null ? match.getCourt().getName() : null,
            inningsDetails,
            topRuns, topWickets
        );
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsRow> getLeaderboard(Long configId, String type) {
        return switch (type) {
            case "runs" -> statsRepo.findByConfigIdOrderByTotalRunsDesc(configId)
                .stream().map(this::toPlayerStatsRow).toList();
            case "wickets" -> statsRepo.findByConfigIdOrderByTotalWicketsDesc(configId)
                .stream().map(this::toPlayerStatsRow).toList();
            case "mvp" -> statsRepo.findByConfigIdOrderByManOfMatchCountDesc(configId)
                .stream().map(this::toPlayerStatsRow).toList();
            default -> Collections.emptyList();
        };
    }

    @Transactional(readOnly = true)
    public List<PlayerStatsRow> getPlayerCareerStats(Long playerId) {
        return statsRepo.findByPlayerId(playerId).stream()
            .map(this::toPlayerStatsRow).toList();
    }

    private void recalculatePlayerStats(Long configId, TournamentMatch match) {
        Set<Long> playerIds = new HashSet<>();

        List<MatchResult> results = resultRepo.findByMatch_Config_Id(configId);

        for (MatchResult r : results) {
            List<MatchInnings> inns = inningsRepo.findByMatchResultIdOrderByInningsNumber(r.getId());
            for (MatchInnings inn : inns) {
                battingRepo.findByInningsIdOrderByBattingPosition(inn.getId())
                    .forEach(bp -> playerIds.add(bp.getPlayer().getId()));
                bowlingRepo.findByInningsIdOrderByBowlingOrder(inn.getId())
                    .forEach(bp -> playerIds.add(bp.getPlayer().getId()));
            }

            if (r.getManOfMatch() != null) playerIds.add(r.getManOfMatch().getId());
        }

        for (Long playerId : playerIds) {
            recalculateSinglePlayer(configId, playerId);
        }
    }

    private void recalculateSinglePlayer(Long configId, Long playerId) {
        List<BattingPerformance> allBatting = battingRepo.findByPlayerIdAndInnings_MatchResult_Match_Config_Id(playerId, configId);
        List<BowlingPerformance> allBowling = bowlingRepo.findByPlayerIdAndInnings_MatchResult_Match_Config_Id(playerId, configId);

        AuctionPlayer player = playerRepo.findById(playerId).orElse(null);
        if (player == null) return;
        TournamentConfig config = configRepo.findById(configId).orElse(null);
        if (config == null) return;

        int totalRuns = allBatting.stream().mapToInt(BattingPerformance::getRunsScored).sum();
        int totalBalls = allBatting.stream().mapToInt(BattingPerformance::getBallsFaced).sum();
        int highScore = allBatting.stream().mapToInt(BattingPerformance::getRunsScored).max().orElse(0);
        int totalFours = allBatting.stream().mapToInt(BattingPerformance::getFours).sum();
        int totalSixes = allBatting.stream().mapToInt(BattingPerformance::getSixes).sum();
        int fifties = (int) allBatting.stream().filter(b -> b.getRunsScored() >= 50 && b.getRunsScored() < 100).count();
        int hundreds = (int) allBatting.stream().filter(b -> b.getRunsScored() >= 100).count();
        long dismissals = allBatting.stream().filter(b -> b.getDismissalType() != null && b.getDismissalType() != DismissalType.NOT_OUT).count();

        int totalWickets = allBowling.stream().mapToInt(BowlingPerformance::getWicketsTaken).sum();
        int totalRunsConceded = allBowling.stream().mapToInt(BowlingPerformance::getRunsConceded).sum();
        BigDecimal totalOversBowled = allBowling.stream()
            .map(BowlingPerformance::getOversBowled)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String bestBowling = allBowling.stream()
            .max(Comparator.comparingInt(BowlingPerformance::getWicketsTaken)
                .thenComparing(b -> -b.getRunsConceded()))
            .map(b -> b.getWicketsTaken() + "/" + b.getRunsConceded())
            .orElse(null);

        BigDecimal batAvg = dismissals > 0
            ? new BigDecimal(totalRuns).divide(new BigDecimal(dismissals), 2, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(totalRuns);
        BigDecimal batSR = totalBalls > 0
            ? new BigDecimal(totalRuns * 100).divide(new BigDecimal(totalBalls), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal bowlAvg = totalWickets > 0
            ? new BigDecimal(totalRunsConceded).divide(new BigDecimal(totalWickets), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        BigDecimal eco = totalOversBowled.compareTo(BigDecimal.ZERO) > 0
            ? new BigDecimal(totalRunsConceded).divide(totalOversBowled, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        long momCount = resultRepo.findByMatch_Config_Id(configId).stream()
            .filter(r -> r.getManOfMatch() != null && r.getManOfMatch().getId().equals(playerId))
            .count();

        Set<Long> matchIds = new HashSet<>();
        allBatting.forEach(b -> matchIds.add(b.getInnings().getMatchResult().getMatch().getId()));
        allBowling.forEach(b -> matchIds.add(b.getInnings().getMatchResult().getMatch().getId()));

        long fielderCatches = battingRepo.countByFielderIdAndDismissalTypeAndConfigId(playerId, DismissalType.CAUGHT, configId);

        PlayerTournamentStats stats = statsRepo.findByConfigIdAndPlayerId(configId, playerId)
            .orElseGet(() -> PlayerTournamentStats.builder().config(config).player(player).build());

        stats.setMatchesPlayed(matchIds.size());
        stats.setInningsBatted(allBatting.size());
        stats.setInningsBowled(allBowling.size());
        stats.setTotalRuns(totalRuns);
        stats.setHighestScore(highScore);
        stats.setBattingAverage(batAvg);
        stats.setBattingStrikeRate(batSR);
        stats.setFifties(fifties);
        stats.setHundreds(hundreds);
        stats.setFours(totalFours);
        stats.setSixes(totalSixes);
        stats.setTotalWickets(totalWickets);
        stats.setBestBowling(bestBowling);
        stats.setBowlingAverage(bowlAvg);
        stats.setEconomyRate(eco);
        stats.setCatches((int) fielderCatches);
        stats.setManOfMatchCount((int) momCount);

        statsRepo.save(stats);
    }

    private InningsDetail toInningsDetail(MatchInnings inn, List<BattingPerformance> batPerfs, List<BowlingPerformance> bowlPerfs) {
        return new InningsDetail(
            inn.getInningsNumber(),
            inn.getBattingTeam().getTeamName(),
            inn.getBowlingTeam().getTeamName(),
            inn.getTotalRuns(), inn.getTotalWickets(),
            inn.getTotalOvers().toPlainString(),
            inn.getExtras(), inn.getExtrasDetail(),
            inn.getTarget(),
            inn.getRunRate().toPlainString(),
            batPerfs.stream().map(this::toBattingRow).toList(),
            bowlPerfs.stream().map(this::toBowlingRow).toList(),
            inn.getFallOfWickets()
        );
    }

    private BattingRow toBattingRow(BattingPerformance bp) {
        return new BattingRow(
            bp.getPlayer().getId(), bp.getPlayer().getPlayerName(),
            bp.getBattingPosition(), bp.getRunsScored(), bp.getBallsFaced(),
            bp.getFours(), bp.getSixes(), bp.getStrikeRate().toPlainString(),
            bp.getDismissalType() != null ? bp.getDismissalType().name() : null,
            bp.getDismissedBy() != null ? bp.getDismissedBy().getPlayerName() : null,
            bp.getFielder() != null ? bp.getFielder().getPlayerName() : null
        );
    }

    private BowlingRow toBowlingRow(BowlingPerformance bp) {
        return new BowlingRow(
            bp.getPlayer().getId(), bp.getPlayer().getPlayerName(),
            bp.getBowlingOrder(), bp.getOversBowled().toPlainString(),
            bp.getMaidens(), bp.getRunsConceded(), bp.getWicketsTaken(),
            bp.getEconomyRate().toPlainString(),
            bp.getDotBalls(), bp.getWides(), bp.getNoBalls()
        );
    }

    private PlayerBrief toPlayerBrief(AuctionPlayer p) {
        String teamName = p.getAssignedTeam() != null ? p.getAssignedTeam().getTeamName() : "";
        return new PlayerBrief(p.getId(), p.getPlayerName(), teamName);
    }

    private PlayerStatsRow toPlayerStatsRow(PlayerTournamentStats s) {
        String teamName = s.getPlayer().getAssignedTeam() != null ? s.getPlayer().getAssignedTeam().getTeamName() : "";
        return new PlayerStatsRow(
            s.getPlayer().getId(), s.getPlayer().getPlayerName(), teamName,
            s.getMatchesPlayed(), s.getTotalRuns(), s.getTotalWickets(),
            s.getBattingAverage().toPlainString(), s.getBattingStrikeRate().toPlainString(),
            s.getEconomyRate().toPlainString(), s.getManOfMatchCount()
        );
    }

    private BigDecimal parsOvers(String overs) {
        if (overs == null || overs.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(overs);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}


