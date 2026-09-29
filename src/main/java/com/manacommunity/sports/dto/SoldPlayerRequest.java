package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import jakarta.validation.constraints.NotNull;

public record SoldPlayerRequest(
    @NotNull Long playerId,
    @NotNull Long teamId
) {}

