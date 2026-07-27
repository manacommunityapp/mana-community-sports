package com.manacommunity.sports.dto;

import jakarta.validation.constraints.*;

public record AuctionConfigRequest(
    @NotNull Long sportId,
    Long eventId,
    @NotBlank String seasonName,
    @NotBlank String auctionFormat,

    @Min(2) @Max(20)  Integer totalTeams,
    @Min(1)           Integer totalPlayers,
    @Min(1000)        Long    budgetPerTeam,

    // Bidding rules — all configurable
    @Min(100)         Integer basePrice,
    @Min(100)         Integer bidIncrementDefault,
    @Min(0)           Long    bidIncrementThreshold,
    @Min(100)         Integer bidIncrementAbove,
    @Min(10) @Max(120)Integer bidTimerSeconds,

    Boolean     rtmEnabled,
    String      unsoldRule,

    java.util.List<String> categories,          // ["BATSMEN","BOWLERS","ALL_ROUNDERS"]
    java.util.List<String> committeeMembers     // ["Ramesh","Sandeep","Chetan","Sunil"]
) {}
