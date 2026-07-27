package com.manacommunity.sports.dto;

public record PlayerWithBidResponse(
    Long    playerId,
    String  playerName,
    String  category,
    String  playerRole,
    Integer age,
    Integer basePrice,
    String  statsJson,
    Long    currentBid,
    Long    nextBid,
    Integer nextIncrement,
    String  currentBidTeamName,
    int     queueOrder,
    String  status
) {}
