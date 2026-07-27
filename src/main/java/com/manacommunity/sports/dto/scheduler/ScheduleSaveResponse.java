package com.manacommunity.sports.dto.scheduler;

/**
 * Result of a unified schedule save: the persisted config and how many matches
 * were committed.
 */
public record ScheduleSaveResponse(TournamentConfigResponse config, int savedMatches) {}
