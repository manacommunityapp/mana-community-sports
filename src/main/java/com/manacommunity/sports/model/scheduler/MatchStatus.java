package com.manacommunity.sports.model.scheduler;

public enum MatchStatus {
    DRAFT,        // saved as draft — visible only to the organiser
    PUBLISHED,    // published — visible to all participants
    SCHEDULED,
    LIVE,
    COMPLETED,
    POSTPONED,
    CANCELLED,
    BYE,          // auto-advance when team count is not a power of 2
    AUTO_ADVANCED // BYE match whose seeded player was auto-progressed to the next round
}
