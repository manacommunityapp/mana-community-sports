package com.manacommunity.sports.dto.scheduler;

import java.util.List;

public record GroupResponse(
    Long                       groupId,
    String                     groupName,
    List<StandingResponse>     standings,
    List<MatchResponse>        matches
) {}
