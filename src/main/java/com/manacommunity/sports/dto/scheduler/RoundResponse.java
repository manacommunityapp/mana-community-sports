package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
import java.util.List;

public record RoundResponse(
    String              roundName,
    int                 roundNumber,
    List<MatchResponse> matches
) {}

