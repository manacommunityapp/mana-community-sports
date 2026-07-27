package com.manacommunity.sports.dto.scheduler;

import java.util.List;

public record RoundResponse(
    String              roundName,
    int                 roundNumber,
    List<MatchResponse> matches
) {}
