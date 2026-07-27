package com.manacommunity.sports.dto.scheduler;

import jakarta.validation.constraints.NotBlank;

public record RescheduleRequest(
    @NotBlank String scheduledAt,
    String venue
) {}
