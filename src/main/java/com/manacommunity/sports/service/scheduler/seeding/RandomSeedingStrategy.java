package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.sports.model.Community;

import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RANDOM — securely shuffle every player, then pair neighbours:
 * <pre>
 *   P5 vs P2
 *   P8 vs P1
 *   P4 vs P7
 *   P6 vs P3
 * </pre>
 *
 * <ul>
 *   <li><b>Secure randomization</b> — uses {@link SecureRandom} (not {@code java.util.Random}).</li>
 *   <li><b>No duplicate pairings</b> — a shuffle is a permutation, so every player appears in
 *       exactly one pairing and no two players meet twice.</li>
 *   <li><b>Community rules (optional)</b> — when enabled, re-shuffles up to {@link #MAX_ATTEMPTS}
 *       times to avoid same-flat / same-tower clashes, then repairs any residual clash with a
 *       local swap (best-effort if the draw makes a clash unavoidable).</li>
 * </ul>
 */
@Component
public class RandomSeedingStrategy implements SeedingStrategy {

    private static final int MAX_ATTEMPTS = 50;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public ScheduleSequence type() {
        return ScheduleSequence.RANDOM;
    }

    @Override
    public List<Pairing> firstRoundPairings(List<ParticipantRef> players, boolean communityRulesEnabled) {
        if (players.size() < 2) {
            return SeedingStrategy.neighborPairs(players);
        }

        List<Pairing> lastAttempt = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            List<ParticipantRef> shuffled = new ArrayList<>(players);
            Collections.shuffle(shuffled, secureRandom);          // secure randomization
            List<Pairing> pairs = SeedingStrategy.neighborPairs(shuffled);

            if (!communityRulesEnabled || !hasConflict(pairs)) {
                return pairs;                                     // clean (or rules off)
            }
            lastAttempt = pairs;
        }

        // Couldn't find a clash-free shuffle — repair the last one as best we can.
        repair(lastAttempt);
        return lastAttempt;
    }

    /** True when any non-BYE pair violates a community rule. */
    private boolean hasConflict(List<Pairing> pairs) {
        for (Pairing p : pairs) {
            if (!p.isBye() && CommunityRules.conflict(p.home(), p.away())) return true;
        }
        return false;
    }

    /**
     * Local repair: for each conflicting pair, swap its away player with another pair's away
     * player when doing so removes the clash from both. Leaves a pair unchanged if no valid
     * swap exists (mirrors the bracket generator's best-effort constraint pass).
     */
    private void repair(List<Pairing> pairs) {
        for (int i = 0; i < pairs.size(); i++) {
            Pairing pi = pairs.get(i);
            if (pi.isBye() || !CommunityRules.conflict(pi.home(), pi.away())) continue;
            for (int j = 0; j < pairs.size(); j++) {
                if (j == i) continue;
                Pairing pj = pairs.get(j);
                if (pj.isBye()) continue;
                if (!CommunityRules.conflict(pi.home(), pj.away())
                        && !CommunityRules.conflict(pj.home(), pi.away())) {
                    pairs.set(i, new Pairing(pi.home(), pj.away()));
                    pairs.set(j, new Pairing(pj.home(), pi.away()));
                    break;
                }
            }
        }
    }
}
