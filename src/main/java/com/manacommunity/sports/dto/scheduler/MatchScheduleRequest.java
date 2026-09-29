package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
public record MatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime) {}

