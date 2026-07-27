package com.manacommunity.sports.dto;

public record AuctionStatsResponse(
    long totalPlayers,
    long soldPlayers,
    long queuedPlayers,
    long totalTeams,
    long totalBudget,
    long totalSpent
) {}
