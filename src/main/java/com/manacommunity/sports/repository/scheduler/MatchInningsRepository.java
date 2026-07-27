package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.sports.model.scheduler.MatchInnings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MatchInningsRepository extends JpaRepository<MatchInnings, Long> {
    List<MatchInnings> findByMatchResultIdOrderByInningsNumber(Long matchResultId);
}
