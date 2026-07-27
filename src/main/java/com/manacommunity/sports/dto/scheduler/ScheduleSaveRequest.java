package com.manacommunity.sports.dto.scheduler;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Unified, deferred save payload: the tournament config plus the full set of
 * customized matches, persisted together in a single transaction by
 * POST /api/tournament/schedule/save.
 */
public record ScheduleSaveRequest(
    Long                                 configId,   // null => create a new config
    String                               status,     // "DRAFT" | "PUBLISHED"
    @Valid TournamentConfigRequest       config,
    List<BulkMatchSaveRequest.MatchData> matches
) {}
