package com.manacommunity.sports.dto.scheduler;

public record MatchResponse(
    Long    matchId,
    String  roundName,
    int     matchNumber,
    int     bracketSlot,
    Long    teamAId,
    String  teamAName,
    Long    teamBId,
    String  teamBName,
    String  teamAColor,
    String  teamBColor,
    String  scheduledAt,
    Long    venueId,
    String  venueName,
    Long    courtId,
    String  courtName,
    String  status,
    String  scoreTeamA,
    String  scoreTeamB,
    String  winnerName,
    Long    winnerAdvancesToMatchId,
    boolean isBye
) {}
