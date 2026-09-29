package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.scheduler.MatchBallEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchBallEventRepository extends JpaRepository<MatchBallEvent, Long> {

    List<MatchBallEvent> findByMatchIdAndIsUndoneFalseOrderByDeliveryNumber(Long matchId);

    List<MatchBallEvent> findByMatchIdAndInningsNumberAndIsUndoneFalseOrderByDeliveryNumber(Long matchId, Integer inningsNumber);

    List<MatchBallEvent> findByMatchIdOrderByDeliveryNumberDesc(Long matchId);
}

