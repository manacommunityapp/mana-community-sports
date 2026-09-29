package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.SportsScheduleStatsResponse;
import com.manacommunity.common.user.model.AppUser;

public interface SportsScheduleAllEventsService {
    SportsScheduleStatsResponse getStats(AppUser user);
    long getTotalGames(AppUser user);
    long getLiveGames(AppUser user);
    long getUpcomingGames(AppUser user);
    long getCompletedGames(AppUser user);
}


