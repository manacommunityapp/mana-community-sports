package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
public record SportsScheduleStatsResponse(
        long totalGames,
        long liveNow,
        long upcoming,
        long completed
) {}

