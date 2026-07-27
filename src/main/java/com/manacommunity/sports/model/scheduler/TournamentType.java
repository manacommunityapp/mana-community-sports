package com.manacommunity.sports.model.scheduler;

public enum TournamentType {
    /** Single elimination — lose once and you're out */
    KNOCKOUT,

    /** Groups (A, B …) → top N per group advance to knockout */
    GROUP_KNOCKOUT,

    /** Every team plays every other team once (league table) */
    ROUND_ROBIN,

    /** Winners bracket + Losers bracket — need two losses to exit */
    DOUBLE_ELIMINATION,

    /** Paired by score each round — no repeat match-ups */
    SWISS,

    /** Top teams play more games; designed like IPL */
    SUPER_LEAGUE,

    // ── Aliases sent from the SportsEvent UI ──────────────────────

    /** Alias for KNOCKOUT (single-elimination) */
    KNOCKOUT_SINGLE,

    /** Alias for DOUBLE_ELIMINATION */
    KNOCKOUT_DOUBLE,

    /** Alias for GROUP_KNOCKOUT */
    GROUP_PLAYOFF,

    /** Alias for ROUND_ROBIN */
    LEAGUE,

    /** Custom format — falls back to single-elimination scheduling */
    CUSTOM
}
