package com.manacommunity.sports.dto;

import lombok.Data;

/**
 * BUG FIX: AuctionController referenced AuctionTeamSummary but it never existed.
 * Placeholder DTO until proper team summary logic is built.
 */
@Data
public class AuctionTeamSummary{
        Long teamId;
        String teamName;
        String color;
        Integer remainingBudget;
        int playerCount;
}
