package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.scheduler.PlayerTournamentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerTournamentStatsRepository extends JpaRepository<PlayerTournamentStats, Long> {
    Optional<PlayerTournamentStats> findByConfigIdAndPlayerId(Long configId, Long playerId);
    List<PlayerTournamentStats> findByConfigIdOrderByTotalRunsDesc(Long configId);
    List<PlayerTournamentStats> findByConfigIdOrderByTotalWicketsDesc(Long configId);
    List<PlayerTournamentStats> findByConfigIdOrderByManOfMatchCountDesc(Long configId);
    List<PlayerTournamentStats> findByPlayerId(Long playerId);
}

