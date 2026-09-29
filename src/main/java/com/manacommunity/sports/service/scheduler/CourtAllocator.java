package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.Court;
import com.manacommunity.sports.model.scheduler.TournamentConfig;
import com.manacommunity.sports.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Allocates courts to generated matches, distributing them round-robin across
 * the config venue's courts so every court is used (mirrors the UI's
 * court-distribution behaviour). Returns null when the venue has no courts.
 */
@Service
@RequiredArgsConstructor
public class CourtAllocator {

    private final CourtRepository courtRepo;

    /** Courts available at the config's venue (empty if no venue / no courts). */
    public List<Court> courtsFor(TournamentConfig config) {
        if (config == null || config.getVenue() == null) return List.of();
        return courtRepo.findByVenueId(config.getVenue().getId());
    }

    /** Round-robin pick from a pre-fetched court list by match index. */
    public Court pick(List<Court> courts, int index) {
        if (courts == null || courts.isEmpty()) return null;
        return courts.get(Math.floorMod(index, courts.size()));
    }

    /** Convenience: allocate a court for the given match index (queries per call). */
    public Court allocate(TournamentConfig config, int index) {
        return pick(courtsFor(config), index);
    }
}

