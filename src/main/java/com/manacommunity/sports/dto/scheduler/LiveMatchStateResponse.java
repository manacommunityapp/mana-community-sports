package com.manacommunity.sports.dto.scheduler;

import java.util.List;

public record LiveMatchStateResponse(
    Long matchId,
    String status,
    Integer currentInnings,
    InningsState innings1,
    InningsState innings2,
    Long batsmanOnStrikeId,
    String batsmanOnStrikeName,
    Long batsmanNonStrikeId,
    String batsmanNonStrikeName,
    Long currentBowlerId,
    String currentBowlerName,
    List<BallEventResponse> recentBalls,
    String teamAName,
    String teamBName,
    String teamAColor,
    String teamBColor,
    Long teamAId,
    Long teamBId,
    Integer target
) {
    public record InningsState(
        Long battingTeamId,
        String battingTeamName,
        Integer totalRuns,
        Integer totalWickets,
        String totalOvers,
        String runRate,
        List<BatterState> batters,
        List<BowlerState> bowlers,
        List<String> thisOver
    ) {}

    public record BatterState(
        Long playerId,
        String playerName,
        Integer runs,
        Integer balls,
        Integer fours,
        Integer sixes,
        String strikeRate,
        Boolean isOut,
        String dismissalText
    ) {}

    public record BowlerState(
        Long playerId,
        String playerName,
        String overs,
        Integer runs,
        Integer wickets,
        Integer maidens,
        String economy,
        Integer dots
    ) {}
}
