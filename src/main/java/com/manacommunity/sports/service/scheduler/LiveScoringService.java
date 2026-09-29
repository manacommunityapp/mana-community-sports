package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.scheduler.*;
import com.manacommunity.sports.dto.scheduler.LiveMatchStateResponse.*;
import com.manacommunity.common.exception.ResourceNotFoundException;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.scheduler.*;
import com.manacommunity.sports.repository.AuctionPlayerRepository;
import com.manacommunity.sports.repository.scheduler.MatchBallEventRepository;
import com.manacommunity.sports.repository.scheduler.TournamentMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LiveScoringService {

    private final MatchBallEventRepository ballRepo;
    private final TournamentMatchRepository matchRepo;
    private final AuctionPlayerRepository playerRepo;

    @Transactional
    public BallEventResponse recordBall(BallEventRequest req, Long userId) {
        TournamentMatch match = matchRepo.findById(req.matchId())
            .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", req.matchId()));

        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartedAt() == null) match.setStartedAt(LocalDateTime.now());
            matchRepo.save(match);
        }

        List<MatchBallEvent> existing = ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(
            req.matchId(), req.inningsNumber());

        int deliveryNumber = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getDeliveryNumber() + 1;

        int prevRuns = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getTotalRuns();
        int prevWickets = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getTotalWickets();

        int ballRuns = (req.runsScored() != null ? req.runsScored() : 0)
                     + (req.extrasRuns() != null ? req.extrasRuns() : 0);
        int newTotalRuns = prevRuns + ballRuns;

        boolean isWicket = Boolean.TRUE.equals(req.isWicket());
        int newTotalWickets = prevWickets + (isWicket ? 1 : 0);

        boolean isLegalDelivery = isLegalDelivery(req.extrasType());
        int legalBalls = countLegalBalls(existing) + (isLegalDelivery ? 1 : 0);
        int overNumber = legalBalls > 0 ? (legalBalls - 1) / 6 : 0;
        int ballInOver = legalBalls > 0 ? ((legalBalls - 1) % 6) + 1 : 0;
        String totalOvers = overNumber + "." + ballInOver;

        AuctionPlayer batsman = playerRepo.findById(req.batsmanId())
            .orElseThrow(() -> new ResourceNotFoundException("Player", req.batsmanId()));
        AuctionPlayer bowler = playerRepo.findById(req.bowlerId())
            .orElseThrow(() -> new ResourceNotFoundException("Player", req.bowlerId()));
        AuctionPlayer nonStriker = req.nonStrikerId() != null
            ? playerRepo.findById(req.nonStrikerId()).orElse(null) : null;
        AuctionPlayer dismissedPlayer = req.dismissedPlayerId() != null
            ? playerRepo.findById(req.dismissedPlayerId()).orElse(null) : null;
        AuctionPlayer fielder = req.fielderId() != null
            ? playerRepo.findById(req.fielderId()).orElse(null) : null;

        MatchBallEvent event = MatchBallEvent.builder()
            .match(match)
            .inningsNumber(req.inningsNumber())
            .overNumber(overNumber)
            .ballNumber(ballInOver)
            .deliveryNumber(deliveryNumber)
            .runsScored(req.runsScored() != null ? req.runsScored() : 0)
            .isBoundary(Boolean.TRUE.equals(req.isBoundary()))
            .isSix(Boolean.TRUE.equals(req.isSix()))
            .extrasType(req.extrasType())
            .extrasRuns(req.extrasRuns() != null ? req.extrasRuns() : 0)
            .isWicket(isWicket)
            .dismissalType(req.dismissalType() != null ? DismissalType.valueOf(req.dismissalType()) : null)
            .dismissedPlayer(dismissedPlayer)
            .fielder(fielder)
            .batsman(batsman)
            .nonStriker(nonStriker)
            .bowler(bowler)
            .totalRuns(newTotalRuns)
            .totalWickets(newTotalWickets)
            .totalOvers(totalOvers)
            .commentary(req.commentary())
            .isUndone(false)
            .createdBy(userId)
            .build();

        ballRepo.save(event);

        match.setScoreTeamA(req.inningsNumber() == 1 ? newTotalRuns + "/" + newTotalWickets + " (" + totalOvers + " ov)" : match.getScoreTeamA());
        match.setScoreTeamB(req.inningsNumber() == 2 ? newTotalRuns + "/" + newTotalWickets + " (" + totalOvers + " ov)" : match.getScoreTeamB());
        matchRepo.save(match);

        return toBallEventResponse(event);
    }

    @Transactional
    public BallEventResponse undoLastBall(Long matchId, Integer inningsNumber) {
        List<MatchBallEvent> balls = ballRepo.findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(
            matchId, inningsNumber);
        if (balls.isEmpty()) throw new IllegalStateException("No balls to undo");

        MatchBallEvent last = balls.get(balls.size() - 1);
        last.setIsUndone(true);
        ballRepo.save(last);

        if (balls.size() > 1) {
            MatchBallEvent prev = balls.get(balls.size() - 2);
            TournamentMatch match = last.getMatch();
            String score = prev.getTotalRuns() + "/" + prev.getTotalWickets() + " (" + prev.getTotalOvers() + " ov)";
            if (inningsNumber == 1) match.setScoreTeamA(score);
            else match.setScoreTeamB(score);
            matchRepo.save(match);
        }

        return toBallEventResponse(last);
    }

    @Transactional
    public void startInnings(Long matchId, Integer inningsNumber) {
        TournamentMatch match = matchRepo.findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", matchId));
        if (match.getStatus() != MatchStatus.LIVE) {
            match.setStatus(MatchStatus.LIVE);
            if (match.getStartedAt() == null) match.setStartedAt(LocalDateTime.now());
            matchRepo.save(match);
        }
    }

    @Transactional(readOnly = true)
    public LiveMatchStateResponse getMatchState(Long matchId) {
        TournamentMatch match = matchRepo.findById(matchId)
            .orElseThrow(() -> new ResourceNotFoundException("TournamentMatch", matchId));

        List<MatchBallEvent> allBalls = ballRepo.findByMatchIdAndIsUndoneFalseOrderByDeliveryNumber(matchId);
        List<MatchBallEvent> inn1Balls = allBalls.stream().filter(b -> b.getInningsNumber() == 1).toList();
        List<MatchBallEvent> inn2Balls = allBalls.stream().filter(b -> b.getInningsNumber() == 2).toList();

        int currentInnings = inn2Balls.isEmpty() ? 1 : 2;
        List<MatchBallEvent> currentBalls = currentInnings == 1 ? inn1Balls : inn2Balls;

        Long batsmanOnStrikeId = null;
        String batsmanOnStrikeName = null;
        Long batsmanNonStrikeId = null;
        String batsmanNonStrikeName = null;
        Long currentBowlerId = null;
        String currentBowlerName = null;

        if (!currentBalls.isEmpty()) {
            MatchBallEvent lastBall = currentBalls.get(currentBalls.size() - 1);
            batsmanOnStrikeId = lastBall.getBatsman().getId();
            batsmanOnStrikeName = lastBall.getBatsman().getPlayerName();
            if (lastBall.getNonStriker() != null) {
                batsmanNonStrikeId = lastBall.getNonStriker().getId();
                batsmanNonStrikeName = lastBall.getNonStriker().getPlayerName();
            }
            currentBowlerId = lastBall.getBowler().getId();
            currentBowlerName = lastBall.getBowler().getPlayerName();
        }

        Integer target = null;
        if (!inn1Balls.isEmpty() && !inn2Balls.isEmpty()) {
            target = inn1Balls.get(inn1Balls.size() - 1).getTotalRuns() + 1;
        }

        List<BallEventResponse> recentBalls = currentBalls.stream()
            .skip(Math.max(0, currentBalls.size() - 12))
            .map(this::toBallEventResponse).toList();

        return new LiveMatchStateResponse(
            matchId,
            match.getStatus().name(),
            currentInnings,
            buildInningsState(inn1Balls, match.getTeamA()),
            buildInningsState(inn2Balls, match.getTeamB()),
            batsmanOnStrikeId, batsmanOnStrikeName,
            batsmanNonStrikeId, batsmanNonStrikeName,
            currentBowlerId, currentBowlerName,
            recentBalls,
            match.getTeamA() != null ? match.getTeamA().getTeamName() : "TBD",
            match.getTeamB() != null ? match.getTeamB().getTeamName() : "TBD",
            match.getTeamA() != null ? match.getTeamA().getColorHex() : "#555",
            match.getTeamB() != null ? match.getTeamB().getColorHex() : "#555",
            match.getTeamA() != null ? match.getTeamA().getId() : null,
            match.getTeamB() != null ? match.getTeamB().getId() : null,
            target
        );
    }

    private InningsState buildInningsState(List<MatchBallEvent> balls, com.manacommunity.sports.model.AuctionTeam team) {
        if (balls.isEmpty()) {
            return new InningsState(
                team != null ? team.getId() : null,
                team != null ? team.getTeamName() : "TBD",
                0, 0, "0.0", "0.00",
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList()
            );
        }

        MatchBallEvent last = balls.get(balls.size() - 1);
        int totalRuns = last.getTotalRuns();
        int totalWickets = last.getTotalWickets();
        String totalOvers = last.getTotalOvers();

        int legalBalls = countLegalBalls(balls);
        BigDecimal oversDecimal = legalBalls > 0
            ? new BigDecimal(legalBalls).divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        String runRate = oversDecimal.compareTo(BigDecimal.ZERO) > 0
            ? new BigDecimal(totalRuns).divide(oversDecimal, 2, RoundingMode.HALF_UP).toPlainString()
            : "0.00";

        Map<Long, BatterState> batters = new LinkedHashMap<>();
        Map<Long, BowlerState> bowlers = new LinkedHashMap<>();

        Map<Long, int[]> batterStats = new LinkedHashMap<>();
        Map<Long, String> batterNames = new LinkedHashMap<>();
        Map<Long, int[]> bowlerStats = new LinkedHashMap<>();
        Map<Long, String> bowlerNames = new LinkedHashMap<>();
        Map<Long, String> dismissals = new HashMap<>();

        for (MatchBallEvent b : balls) {
            Long batId = b.getBatsman().getId();
            batterNames.putIfAbsent(batId, b.getBatsman().getPlayerName());
            int[] bs = batterStats.computeIfAbsent(batId, k -> new int[5]);
            bs[0] += b.getRunsScored();
            if (isLegalDelivery(b.getExtrasType()) || "NO_BALL".equals(b.getExtrasType())) {
                bs[1]++;
            }
            if (Boolean.TRUE.equals(b.getIsBoundary()) && !Boolean.TRUE.equals(b.getIsSix())) bs[2]++;
            if (Boolean.TRUE.equals(b.getIsSix())) bs[3]++;

            if (Boolean.TRUE.equals(b.getIsWicket()) && b.getDismissedPlayer() != null) {
                Long dpId = b.getDismissedPlayer().getId();
                String dt = b.getDismissalType() != null ? b.getDismissalType().name().toLowerCase().replace("_", " ") : "out";
                dismissals.put(dpId, dt);
            }

            Long bowId = b.getBowler().getId();
            bowlerNames.putIfAbsent(bowId, b.getBowler().getPlayerName());
            int[] bw = bowlerStats.computeIfAbsent(bowId, k -> new int[5]);
            bw[0] += b.getRunsScored() + b.getExtrasRuns();
            if (Boolean.TRUE.equals(b.getIsWicket())) bw[1]++;
            if (isLegalDelivery(b.getExtrasType())) bw[2]++;
            if (b.getRunsScored() == 0 && (b.getExtrasRuns() == null || b.getExtrasRuns() == 0) && isLegalDelivery(b.getExtrasType())) bw[3]++;
        }

        List<BatterState> batterList = new ArrayList<>();
        for (var entry : batterStats.entrySet()) {
            Long pid = entry.getKey();
            int[] s = entry.getValue();
            String sr = s[1] > 0 ? new BigDecimal(s[0] * 100).divide(new BigDecimal(s[1]), 2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            boolean isOut = dismissals.containsKey(pid);
            batterList.add(new BatterState(pid, batterNames.get(pid), s[0], s[1], s[2], s[3], sr, isOut, dismissals.get(pid)));
        }

        List<BowlerState> bowlerList = new ArrayList<>();
        for (var entry : bowlerStats.entrySet()) {
            Long pid = entry.getKey();
            int[] s = entry.getValue();
            int completedOvers = s[2] / 6;
            int partialBalls = s[2] % 6;
            String overs = completedOvers + "." + partialBalls;
            BigDecimal od = s[2] > 0 ? new BigDecimal(s[2]).divide(BigDecimal.valueOf(6), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String eco = od.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal(s[0]).divide(od, 2, RoundingMode.HALF_UP).toPlainString() : "0.00";
            bowlerList.add(new BowlerState(pid, bowlerNames.get(pid), overs, s[0], s[1], 0, eco, s[3]));
        }

        int currentOverStart = balls.stream()
            .filter(b -> isLegalDelivery(b.getExtrasType()))
            .mapToInt(MatchBallEvent::getOverNumber)
            .max().orElse(0);
        List<String> thisOver = balls.stream()
            .filter(b -> b.getOverNumber() >= currentOverStart || !isLegalDelivery(b.getExtrasType()))
            .skip(Math.max(0, balls.stream().filter(b -> b.getOverNumber() < currentOverStart).count()))
            .map(this::formatBallShort)
            .collect(Collectors.toCollection(ArrayList::new));
        List<String> currentOverBalls = new ArrayList<>();
        for (int i = balls.size() - 1; i >= 0; i--) {
            MatchBallEvent b = balls.get(i);
            if (isLegalDelivery(b.getExtrasType()) && b.getOverNumber() < (legalBalls - 1) / 6) break;
            currentOverBalls.add(0, formatBallShort(b));
        }

        return new InningsState(
            team != null ? team.getId() : null,
            team != null ? team.getTeamName() : "TBD",
            totalRuns, totalWickets, totalOvers, runRate,
            batterList, bowlerList, currentOverBalls
        );
    }

    private String formatBallShort(MatchBallEvent b) {
        if (Boolean.TRUE.equals(b.getIsWicket())) return "W";
        if (b.getExtrasType() != null) {
            return switch (b.getExtrasType()) {
                case "WIDE" -> b.getExtrasRuns() + "wd";
                case "NO_BALL" -> b.getExtrasRuns() + "nb";
                case "BYE" -> b.getExtrasRuns() + "b";
                case "LEG_BYE" -> b.getExtrasRuns() + "lb";
                default -> String.valueOf(b.getRunsScored());
            };
        }
        if (Boolean.TRUE.equals(b.getIsSix())) return "6";
        if (Boolean.TRUE.equals(b.getIsBoundary())) return "4";
        return String.valueOf(b.getRunsScored());
    }

    private boolean isLegalDelivery(String extrasType) {
        return extrasType == null || (!extrasType.equals("WIDE") && !extrasType.equals("NO_BALL"));
    }

    private int countLegalBalls(List<MatchBallEvent> balls) {
        return (int) balls.stream().filter(b -> isLegalDelivery(b.getExtrasType())).count();
    }

    private BallEventResponse toBallEventResponse(MatchBallEvent e) {
        return new BallEventResponse(
            e.getId(), e.getMatch().getId(),
            e.getInningsNumber(), e.getOverNumber(), e.getBallNumber(), e.getDeliveryNumber(),
            e.getRunsScored(), e.getIsBoundary(), e.getIsSix(),
            e.getExtrasType(), e.getExtrasRuns(),
            e.getIsWicket(),
            e.getDismissalType() != null ? e.getDismissalType().name() : null,
            e.getDismissedPlayer() != null ? e.getDismissedPlayer().getId() : null,
            e.getDismissedPlayer() != null ? e.getDismissedPlayer().getPlayerName() : null,
            e.getFielder() != null ? e.getFielder().getId() : null,
            e.getFielder() != null ? e.getFielder().getPlayerName() : null,
            e.getBatsman().getId(), e.getBatsman().getPlayerName(),
            e.getNonStriker() != null ? e.getNonStriker().getId() : null,
            e.getNonStriker() != null ? e.getNonStriker().getPlayerName() : null,
            e.getBowler().getId(), e.getBowler().getPlayerName(),
            e.getTotalRuns(), e.getTotalWickets(), e.getTotalOvers(),
            e.getCommentary(),
            e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
        );
    }
}


