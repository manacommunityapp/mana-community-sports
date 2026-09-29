package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
import java.util.List;

public record MatchDetailResponse(
    Long matchId,
    String roundName,
    int matchNumber,
    String scheduledAt,
    String status,
    // Teams
    TeamInfo teamA,
    TeamInfo teamB,
    // Result
    String resultType,
    String scoreTeamA,
    String scoreTeamB,
    String winMargin,
    String winnerName,
    String matchSummary,
    String matchNotes,
    // Toss
    String tossWinnerName,
    String tossDecision,
    // Awards
    PlayerBrief manOfMatch,
    PlayerBrief bestBatter,
    PlayerBrief bestBowler,
    String umpires,
    // Venue
    String venueName,
    String courtName,
    // Scorecard
    List<InningsDetail> innings,
    // Leaderboard snippet for this tournament
    List<PlayerStatsRow> topRunScorers,
    List<PlayerStatsRow> topWicketTakers
) {
    public record TeamInfo(Long id, String name, String color) {}

    public record PlayerBrief(Long id, String name, String teamName) {}

    public record InningsDetail(
        int inningsNumber,
        String battingTeamName,
        String bowlingTeamName,
        int totalRuns,
        int totalWickets,
        String totalOvers,
        int extras,
        String extrasDetail,
        Integer target,
        String runRate,
        List<BattingRow> batting,
        List<BowlingRow> bowling,
        String fallOfWickets
    ) {}

    public record BattingRow(
        Long playerId,
        String playerName,
        int battingPosition,
        int runsScored,
        int ballsFaced,
        int fours,
        int sixes,
        String strikeRate,
        String dismissalType,
        String dismissedBy,
        String fielder
    ) {}

    public record BowlingRow(
        Long playerId,
        String playerName,
        int bowlingOrder,
        String oversBowled,
        int maidens,
        int runsConceded,
        int wicketsTaken,
        String economyRate,
        int dotBalls,
        int wides,
        int noBalls
    ) {}

    public record PlayerStatsRow(
        Long playerId,
        String playerName,
        String teamName,
        int matchesPlayed,
        int totalRuns,
        int totalWickets,
        String battingAverage,
        String strikeRate,
        String economyRate,
        int manOfMatchCount
    ) {}
}

