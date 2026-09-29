package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record AuctionTeamRequest(
    @NotNull Long configId,
    @NotBlank String teamName,
    @NotBlank String ownerName,
    Long ownerUserId,
    String colorHex,
    @NotNull @Min(1000) Long totalBudget
) {}

