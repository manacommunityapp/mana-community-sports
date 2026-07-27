package com.manacommunity.sports.service.scheduler.seeding;

/**
 * The "Choose Your Schedule Sequence" option selected in the UI. Each value maps
 * to a dedicated {@link SeedingStrategy} that pairs players differently:
 *
 * <ul>
 *   <li>{@link #RANDOM} — securely shuffle players, then pair (community rules optional).</li>
 *   <li>{@link #TRADITIONAL} — seed by rank; top seeds kept apart; BYEs to the top seeds.</li>
 *   <li>{@link #SEQUENTIAL} — pair strictly in registration order, no randomization.</li>
 * </ul>
 */
public enum ScheduleSequence {
    RANDOM,
    TRADITIONAL,
    SEQUENTIAL;

    /**
     * Resolves the raw UI string ("Random" / "Traditional" / "Sequential", any case)
     * to an enum value. Blank/unknown defaults to {@link #SEQUENTIAL} are rejected —
     * callers should pass a real selection.
     */
    public static ScheduleSequence fromUi(String value) {
        if (value == null || value.isBlank()) {
            throw new com.manacommunity.sports.exception.InvalidInputException(
                    "Schedule sequence is required (Random | Traditional | Sequential)");
        }
        return switch (value.trim().toUpperCase()) {
            case "RANDOM"                               -> RANDOM;
            case "TRADITIONAL", "SEEDED",
                 "TRADITIONAL (SEEDED)"                 -> TRADITIONAL;
            case "SEQUENTIAL"                           -> SEQUENTIAL;
            default -> throw new com.manacommunity.sports.exception.InvalidInputException(
                    "Unknown schedule sequence: '" + value + "'");
        };
    }
}
