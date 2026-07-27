package com.manacommunity.sports.dto.scheduler;

public record MatchScheduleRequest(Long homeTeamId, Long awayTeamId, String matchType, String stage, String startTime) {}
