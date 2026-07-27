package com.manacommunity.sports.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BidRequest(
    @NotNull Long configId,
    @NotNull Long playerId,
    @NotNull Long teamId,
    @NotNull @Min(1) Long bidAmount,
    Boolean isRtm
) {}
