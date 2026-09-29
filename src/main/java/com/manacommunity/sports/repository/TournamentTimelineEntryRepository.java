package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.TournamentTimelineEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentTimelineEntryRepository extends JpaRepository<TournamentTimelineEntry, Long> {

    List<TournamentTimelineEntry> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}

