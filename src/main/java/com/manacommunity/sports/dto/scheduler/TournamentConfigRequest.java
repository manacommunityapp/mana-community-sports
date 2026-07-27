package com.manacommunity.sports.dto.scheduler;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public record TournamentConfigRequest(
    @NotBlank  String        tournamentName,
    @NotNull   Long          sportId,
    @NotNull   Long          communityId,
    Long                     eventId,            // optional: linked SportsEvent
    @NotNull   String        tournamentType,   // TournamentType enum name

    @Min(2) @Max(64) Integer totalTeams,
    List<Long>               teamIds,          // ordered list of team IDs (optional for save-only)

    // Group stage
    Integer  numberOfGroups,
    Integer  teamsAdvancingPerGroup,

    // Swiss
    Integer  swissRounds,

    // Match options
    Boolean  thirdPlaceMatch,
    Boolean  hasSeeding,

    // Schedule
    @NotNull LocalDate startDate,
    LocalDate          endDate,
    Integer  matchDurationMinutes,
    Integer  breakBetweenMatchesMinutes,
    Long     venueId,

    // Points
    Integer  pointsForWin,
    Integer  pointsForDraw,
    Integer  pointsForLoss
) {}
