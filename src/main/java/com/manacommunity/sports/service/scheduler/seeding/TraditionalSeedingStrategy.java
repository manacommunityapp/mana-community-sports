package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TRADITIONAL (SEEDED) — pair by seed rank using the standard single-elimination
 * bracket so the highest-ranked players cannot meet until the latest rounds:
 * <pre>
 *   Seed1 vs Seed8
 *   Seed4 vs Seed5
 *   Seed2 vs Seed7
 *   Seed3 vs Seed6
 * </pre>
 *
 * <ul>
 *   <li><b>Seed ranking</b> — the input list is the ranking (index 0 = Seed 1).</li>
 *   <li><b>Bracket balance</b> — uses the recursive seed-slot order
 *       ({@code 1,8,4,5,2,7,3,6}…) which separates the top seeds into opposite halves.</li>
 *   <li><b>BYE assignment</b> — when the field isn't a power of two, the missing low seeds
 *       collapse to BYEs that land on the <i>top</i> seeds (e.g. 13 players → seeds 1, 2, 3 get
 *       a BYE), so the strongest players auto-advance round 1.</li>
 * </ul>
 */
@Component
public class TraditionalSeedingStrategy implements SeedingStrategy {

    @Override
    public ScheduleSequence type() {
        return ScheduleSequence.TRADITIONAL;
    }

    @Override
    public List<Pairing> firstRoundPairings(List<ParticipantRef> seededPlayers, boolean communityRulesEnabled) {
        int n = seededPlayers.size();
        if (n < 2) {
            return SeedingStrategy.neighborPairs(seededPlayers);
        }

        int bracketSize = nextPowerOfTwo(n);     // e.g. 13 → 16
        int[] seedOrder = standardSeedOrder(bracketSize);

        List<Pairing> pairs = new ArrayList<>();
        for (int i = 0; i + 1 < seedOrder.length; i += 2) {
            ParticipantRef home = playerForSeed(seededPlayers, seedOrder[i]);
            ParticipantRef away = playerForSeed(seededPlayers, seedOrder[i + 1]);

            // A non-existent low seed (> n) is a BYE. Keep the real player as 'home'.
            if (home == null && away != null) {
                home = away;
                away = null;
            }
            pairs.add(new Pairing(home, away));
        }
        return pairs;
    }

    /** Seed {@code s} (1-based) → player, or null when the seed exceeds the field size (a BYE). */
    private ParticipantRef playerForSeed(List<ParticipantRef> seededPlayers, int seed) {
        return seed <= seededPlayers.size() ? seededPlayers.get(seed - 1) : null;
    }

    /** Smallest power of two ≥ n (n ≥ 1). */
    static int nextPowerOfTwo(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    /**
     * The canonical single-elimination seed-slot order for a bracket of {@code size}
     * (a power of two): {@code size=2 → [1,2]}, {@code 4 → [1,4,2,3]},
     * {@code 8 → [1,8,4,5,2,7,3,6]}, {@code 16 → [1,16,8,9,4,13,5,12,2,15,7,10,3,14,6,11]}.
     * Consecutive entries form the first-round pairings.
     */
    static int[] standardSeedOrder(int size) {
        int[] seeds = {1, 2};
        while (seeds.length < size) {
            int sum = seeds.length * 2 + 1;
            int[] next = new int[seeds.length * 2];
            int idx = 0;
            for (int s : seeds) {
                next[idx++] = s;
                next[idx++] = sum - s;
            }
            seeds = next;
        }
        return seeds;
    }
}

