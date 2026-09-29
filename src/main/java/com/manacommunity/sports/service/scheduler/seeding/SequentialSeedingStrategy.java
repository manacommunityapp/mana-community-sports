package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;

import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * SEQUENTIAL — pair players strictly in registration order, with no randomization:
 * <pre>
 *   P1 vs P2
 *   P3 vs P4
 *   P5 vs P6
 *   P7 vs P8
 * </pre>
 * An odd number of players leaves the last one with a BYE. Community rules are
 * intentionally ignored: the order is fixed by registration.
 */
@Component
public class SequentialSeedingStrategy implements SeedingStrategy {

    @Override
    public ScheduleSequence type() {
        return ScheduleSequence.SEQUENTIAL;
    }

    @Override
    public List<Pairing> firstRoundPairings(List<ParticipantRef> players, boolean communityRulesEnabled) {
        // Registration order is authoritative — pair neighbours as-is.
        return SeedingStrategy.neighborPairs(players);
    }
}


