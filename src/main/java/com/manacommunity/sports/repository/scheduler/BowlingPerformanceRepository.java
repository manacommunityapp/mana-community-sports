package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.sports.model.scheduler.BowlingPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BowlingPerformanceRepository extends JpaRepository<BowlingPerformance, Long> {
    List<BowlingPerformance> findByInningsIdOrderByBowlingOrder(Long inningsId);
    List<BowlingPerformance> findByPlayerIdAndInnings_MatchResult_Match_Config_Id(Long playerId, Long configId);
}
