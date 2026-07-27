package com.manacommunity.sports.dto.scheduler;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MatchResultDetailRequest(
    @NotNull Long matchId,
    Long winnerTeamId,
    String resultType,
    String scoreTeamA,
    String scoreTeamB,
    String winMargin,
    String matchNotes,
    String matchSummary,
    // Toss
    Long tossWinnerTeamId,
    String tossDecision,
    // Awards
    Long manOfMatchPlayerId,
    Long bestBatterPlayerId,
    Long bestBowlerPlayerId,
    String umpires,
    // NRR (for standings)
    Integer runsTeamA,
    Integer runsTeamB,
    Integer oversTeamA,
    Integer oversTeamB,
    // Scorecard (optional — can be submitted later)
    List<InningsRequest> innings
) {
    public record InningsRequest(
        int inningsNumber,
        @NotNull Long battingTeamId,
        @NotNull Long bowlingTeamId,
        int totalRuns,
        int totalWickets,
        String totalOvers,
        int extras,
        String extrasDetail,
        Integer target,
        List<BattingEntry> batting,
        List<BowlingEntry> bowling,
        String fallOfWickets
    ) {}

    public record BattingEntry(
        @NotNull Long playerId,
        int battingPosition,
        int runsScored,
        int ballsFaced,
        int fours,
        int sixes,
        String dismissalType,
        Long dismissedByPlayerId,
        Long fielderPlayerId
    ) {}

    public record BowlingEntry(
        @NotNull Long playerId,
        int bowlingOrder,
        String oversBowled,
        int maidens,
        int runsConceded,
        int wicketsTaken,
        int dotBalls,
        int wides,
        int noBalls
    ) {}
}
