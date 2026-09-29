package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces the <b>first-round pairings</b> for a knockout draw from an ordered list
 * of players. Each concrete strategy corresponds to one {@link ScheduleSequence}
 * the user can pick in the UI.
 *
 * <p>The list ordering convention depends on the strategy:
 * for SEQUENTIAL it is registration order; for TRADITIONAL it is seed rank
 * (index 0 = top seed); for RANDOM the incoming order is irrelevant.</p>
 */
public interface SeedingStrategy {

    /** Which UI option this strategy implements. */
    ScheduleSequence type();

    /**
     * @param players               players to pair (ordering interpreted per {@link #type()})
     * @param communityRulesEnabled when true, honour {@link CommunityRules} in round 1
     * @return ordered first-round pairings (a {@link Pairing#isBye() BYE} has a null away side)
     */
    List<Pairing> firstRoundPairings(List<ParticipantRef> players, boolean communityRulesEnabled);

    /**
     * Pairs an ordered list as neighbours: (0,1),(2,3)… A trailing odd player
     * becomes a BYE. Shared by SEQUENTIAL and RANDOM (after its shuffle).
     */
    static List<Pairing> neighborPairs(List<ParticipantRef> players) {
        List<Pairing> pairs = new ArrayList<>();
        int i = 0;
        for (; i + 1 < players.size(); i += 2) {
            pairs.add(new Pairing(players.get(i), players.get(i + 1)));
        }
        if (i < players.size()) {
            pairs.add(new Pairing(players.get(i), null)); // odd one out → BYE
        }
        return pairs;
    }
}

