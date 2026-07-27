package com.manacommunity.sports.dto.scheduler;

import java.time.LocalDateTime;

/**
 * Response DTO for schedule generation log entries.
 */
public record ScheduleGenerationLogResponse(
    Long id,
    Long configId,
    Long eventId,
    Long communityId,
    Long generatedBy,
    String generatedByName,
    String action,
    String status,
    String tournamentType,
    Integer totalTeams,
    Integer totalMatches,
    Integer totalGroups,
    Long durationMs,
    String errorMessage,
    LocalDateTime createdAt
) {}
