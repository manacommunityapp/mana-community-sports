package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
public record AuctionStatsResponse(
    long totalPlayers,
    long soldPlayers,
    long queuedPlayers,
    long totalTeams,
    long totalBudget,
    long totalSpent
) {}

