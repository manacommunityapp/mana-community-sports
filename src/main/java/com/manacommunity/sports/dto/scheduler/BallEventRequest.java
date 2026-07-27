package com.manacommunity.sports.dto.scheduler;

public record BallEventRequest(
    Long matchId,
    Integer inningsNumber,
    Long batsmanId,
    Long nonStrikerId,
    Long bowlerId,
    Integer runsScored,
    Boolean isBoundary,
    Boolean isSix,
    String extrasType,
    Integer extrasRuns,
    Boolean isWicket,
    String dismissalType,
    Long dismissedPlayerId,
    Long fielderId,
    String commentary
) {}
