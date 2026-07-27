package com.manacommunity.sports.dto;

public record SportsScheduleStatsResponse(
        long totalGames,
        long liveNow,
        long upcoming,
        long completed
) {}
