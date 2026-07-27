package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.sports.model.scheduler.GroupTeamStanding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GroupTeamStandingRepository extends JpaRepository<GroupTeamStanding, Long> {

    @Query("SELECT s FROM GroupTeamStanding s WHERE s.group.id=:gid ORDER BY s.points DESC, s.netRunRate DESC")
    List<GroupTeamStanding> findByGroupIdOrderByPointsDescNetRunRateDesc(@Param("gid") Long groupId);

    @Query("SELECT s FROM GroupTeamStanding s WHERE s.group.id=:gid AND s.team.id=:tid")
    GroupTeamStanding findByGroupIdAndTeamId(@Param("gid") Long groupId, @Param("tid") Long teamId);
}
