package com.manacommunity.sports.repository.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.scheduler.TournamentGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TournamentGroupRepository extends JpaRepository<TournamentGroup, Long> {
    List<TournamentGroup> findByConfigIdOrderByGroupOrder(Long configId);
}

