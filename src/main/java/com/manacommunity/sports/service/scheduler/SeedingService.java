package com.manacommunity.sports.service.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.model.AuctionTeam;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seeding strategy for tournaments: the initial seeding order and the
 * cross-seeding used when group winners feed a knockout bracket.
 */
@Service
public class SeedingService {

    /**
     * Seed teams by ranking/rating: sorts teams based on aggregated squad strength,
     * player ratings, or total spent/budget.
     */
    public List<AuctionTeam> seed(List<AuctionTeam> teams) {
        if (teams == null || teams.size() < 2) return teams;
        List<AuctionTeam> sorted = new ArrayList<>(teams);
        sorted.sort((a, b) -> {
            long scoreA = calculateTeamStrength(a);
            long scoreB = calculateTeamStrength(b);
            return Long.compare(scoreB, scoreA); // descending
        });
        return sorted;
    }

    private long calculateTeamStrength(AuctionTeam team) {
        if (team == null) return 0L;
        long strength = 0L;
        if (team.getSpent() != null) strength += team.getSpent();
        if (team.getTotalBudget() != null) strength += team.getTotalBudget() / 10;
        if (team.getPlayers() != null) {
            strength += team.getPlayers().size() * 1000L;
            for (com.manacommunity.sports.model.AuctionPlayer p : team.getPlayers()) {
                if (p.getSoldPrice() != null) strength += p.getSoldPrice();
                else if (p.getBasePrice() != null) strength += p.getBasePrice();
            }
        }
        return strength;
    }

    /**
     * Standard cross-seeding for group winners: A1 vs B2, B1 vs A2, …
     */
    public List<AuctionTeam> crossSeed(List<AuctionTeam> advancing, int nGroups, int advPer) {
        List<AuctionTeam> result = new ArrayList<>();
        for (int i = 0; i < advPer; i++) {
            for (int g = 0; g < nGroups; g++) {
                int idx = g * advPer + i;
                if (idx < advancing.size()) result.add(advancing.get(idx));
            }
        }
        // Reorder for bracket: A1 vs B2, A2 vs B1
        if (result.size() >= 4) {
            Collections.swap(result, 1, 2);
        }
        return result;
    }
}

