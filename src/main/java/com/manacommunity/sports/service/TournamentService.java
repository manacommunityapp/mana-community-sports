package com.manacommunity.sports.service;

import com.manacommunity.sports.dto.TournamentRequest;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.Tournament;
import java.util.List;

public interface TournamentService {
    List<Tournament> getAllTournaments();
    List<Tournament> getCommunityTournaments(Long communityId);
    Tournament getTournamentById(Long id);
    void deleteTournament(Long id);
    Tournament saveTournamentRecord(TournamentRequest req, Boolean allowAdminChat);
    /** Update an existing tournament in place (identified by id). Never inserts a new record. */
    Tournament updateTournamentRecord(Long id, TournamentRequest req, Boolean allowAdminChat);
    Tournament updateStatus(Long id, String status);
}
