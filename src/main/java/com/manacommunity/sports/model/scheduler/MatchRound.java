package com.manacommunity.sports.model.scheduler;

import com.manacommunity.common.enums.*;
public enum MatchRound {
    // Knockout rounds
    ROUND_OF_64, ROUND_OF_32, ROUND_OF_16,
    QUARTER_FINAL, SEMI_FINAL, THIRD_PLACE, FINAL,

    // Group stage
    GROUP_STAGE,

    // Double elimination
    WINNERS_BRACKET, LOSERS_BRACKET, GRAND_FINAL,

    // Generic
    LEAGUE_MATCH, SWISS_ROUND,

    // Super league
    ELIMINATOR, QUALIFIER_1, QUALIFIER_2, SUPER_FINAL
}

