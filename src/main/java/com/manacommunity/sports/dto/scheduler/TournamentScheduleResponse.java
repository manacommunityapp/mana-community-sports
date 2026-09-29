package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
import java.util.List;

public record TournamentScheduleResponse(
    Long                       configId,
    String                     tournamentName,
    String                     tournamentType,
    int                        totalTeams,
    int                        totalMatches,
    int                        totalRounds,
    java.time.LocalDate        startDate,
    java.time.LocalDate        endDate,
    List<GroupResponse>        groups,
    List<RoundResponse>        rounds,
    List<MatchResponse>        allMatches
) {}

