package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.sports.model.scheduler.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {
    Optional<MatchResult> findByMatchId(Long matchId);
    List<MatchResult> findByMatch_Config_Id(Long configId);
}
