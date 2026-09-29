package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.scheduler.TournamentConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentConfigRepository extends JpaRepository<TournamentConfig, Long> {
    List<TournamentConfig> findByCommunityIdOrderByCreatedAtDesc(Long communityId);
    List<TournamentConfig> findByStatus(TournamentConfig.TournamentStatus status);
}

