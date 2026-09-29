package com.manacommunity.sports.repository;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.TournamentAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TournamentAnnouncementRepository extends JpaRepository<TournamentAnnouncement, Long> {

    List<TournamentAnnouncement> findByTournamentIdOrderBySortOrderAscIdAsc(Long tournamentId);
}

