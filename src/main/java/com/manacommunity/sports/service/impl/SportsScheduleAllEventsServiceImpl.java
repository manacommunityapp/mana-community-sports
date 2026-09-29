package com.manacommunity.sports.service.impl;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.SportsScheduleStatsResponse;
import com.manacommunity.sports.model.scheduler.MatchStatus;
import com.manacommunity.sports.repository.scheduler.SportsScheduleAllEventsRepository;
import com.manacommunity.sports.service.scheduler.SportsScheduleAllEventsService;
import com.manacommunity.common.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.manacommunity.sports.constants.PermissionConstants.ROLE_SUPER_ADMIN;

@Service
@RequiredArgsConstructor
public class SportsScheduleAllEventsServiceImpl implements SportsScheduleAllEventsService {

    private final SportsScheduleAllEventsRepository matchRepo;

    @Override
    @Transactional(readOnly = true)
    public SportsScheduleStatsResponse getStats(AppUser user) {
        long total = getTotalGames(user);
        long live = getLiveGames(user);
        long upcoming = getUpcomingGames(user);
        long completed = getCompletedGames(user);
        return new SportsScheduleStatsResponse(total, live, upcoming, completed);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalGames(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return isSuperAdmin ? matchRepo.count() : (communityId != null ? matchRepo.countByCommunityId(communityId) : 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public long getLiveGames(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return isSuperAdmin ? matchRepo.countByStatus(MatchStatus.LIVE) : (communityId != null ? matchRepo.countByCommunityIdAndStatus(communityId, MatchStatus.LIVE) : 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUpcomingGames(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return isSuperAdmin ? matchRepo.countByStatus(MatchStatus.SCHEDULED) : (communityId != null ? matchRepo.countByCommunityIdAndStatus(communityId, MatchStatus.SCHEDULED) : 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCompletedGames(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return isSuperAdmin ? matchRepo.countByStatus(MatchStatus.COMPLETED) : (communityId != null ? matchRepo.countByCommunityIdAndStatus(communityId, MatchStatus.COMPLETED) : 0L);
    }
}


