package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
import jakarta.validation.constraints.NotBlank;

public record RescheduleRequest(
    @NotBlank String scheduledAt,
    String venue
) {}

