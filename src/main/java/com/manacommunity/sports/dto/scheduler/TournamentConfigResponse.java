package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TournamentConfigResponse(
    Long    id,
    String  tournamentName,
    String  tournamentType,
    Long    sportId,
    String  sportName,
    Long    communityId,
    String  communityName,
    Long    eventId,
    String  eventName,
    Integer totalTeams,
    Integer numberOfGroups,
    Integer teamsPerGroup,
    Integer teamsAdvancingPerGroup,
    Boolean thirdPlaceMatch,
    Boolean hasSeeding,
    Boolean differentFlatEnforced,
    Boolean differentTowerEnforced,
    Boolean ratingBalancingEnabled,
    Integer minRestMinutesBetweenMatches,
    Integer swissRounds,
    LocalDate startDate,
    LocalDate endDate,
    Integer matchDurationMinutes,
    Integer breakBetweenMatchesMinutes,
    Long    venueId,
    String  venueName,
    Integer pointsForWin,
    Integer pointsForDraw,
    Integer pointsForLoss,
    String  status,
    int     totalMatches,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}

