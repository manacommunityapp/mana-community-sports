package com.manacommunity.sports.service.scheduler.seeding;

import com.manacommunity.sports.dto.scheduler.PlayoffMatchDraftResponse.ParticipantRef;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the right {@link SeedingStrategy} from the UI's
 * "Choose Your Schedule Sequence" selection and runs it.
 *
 * <p>Spring injects every {@link SeedingStrategy} bean, so adding a new sequence
 * type is just a new {@code @Component} — no change here.</p>
 *
 * <pre>
 *   // UI sends "Random" | "Traditional" | "Sequential"
 *   List&lt;Pairing&gt; pairs = factory.firstRoundPairings(uiValue, players, communityRulesEnabled);
 * </pre>
 */
@Component
public class SeedingStrategyFactory {

    private final Map<ScheduleSequence, SeedingStrategy> byType = new EnumMap<>(ScheduleSequence.class);

    public SeedingStrategyFactory(List<SeedingStrategy> strategies) {
        for (SeedingStrategy strategy : strategies) {
            byType.put(strategy.type(), strategy);
        }
    }

    /** The strategy bean for a resolved sequence. */
    public SeedingStrategy forSequence(ScheduleSequence sequence) {
        SeedingStrategy strategy = byType.get(sequence);
        if (strategy == null) {
            throw new com.manacommunity.sports.exception.InvalidInputException("No seeding strategy registered for " + sequence);
        }
        return strategy;
    }

    /** Convenience: resolve from the raw UI string and produce the first-round pairings. */
    public List<Pairing> firstRoundPairings(String uiSequenceValue,
                                            List<ParticipantRef> players,
                                            boolean communityRulesEnabled) {
        ScheduleSequence sequence = ScheduleSequence.fromUi(uiSequenceValue);
        return forSequence(sequence).firstRoundPairings(players, communityRulesEnabled);
    }
}
