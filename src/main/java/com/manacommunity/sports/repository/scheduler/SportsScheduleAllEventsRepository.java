package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.model.scheduler.MatchStatus;
import com.manacommunity.sports.model.scheduler.TournamentMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SportsScheduleAllEventsRepository extends JpaRepository<TournamentMatch, Long> {

    @Query("SELECT COUNT(m) FROM TournamentMatch m WHERE m.config.community.id = :communityId")
    long countByCommunityId(@Param("communityId") Long communityId);

    @Query("SELECT COUNT(m) FROM TournamentMatch m WHERE m.config.community.id = :communityId AND m.status = :status")
    long countByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") MatchStatus status);

    long countByStatus(MatchStatus status);
}


