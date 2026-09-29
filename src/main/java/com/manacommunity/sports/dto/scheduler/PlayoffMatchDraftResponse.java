package com.manacommunity.sports.dto.scheduler;

import com.manacommunity.common.enums.*;
/**
 * One generated playoff match (draft). Mirrors the UI's PlayoffMatchDraft
 * (playoffSchedule.ts) so the JSON the frontend already consumes is unchanged.
 */
public record PlayoffMatchDraftResponse(
    String        id,
    String        name,
    String        round,          // QUARTER_FINAL | SEMI_FINAL | FINAL | THIRD_PLACE | "Round N"
    int           roundIndex,
    ParticipantRef home,
    ParticipantRef away,
    String        date,           // "yyyy-MM-dd"
    String        time,           // 12-hour "hh:mm AM/PM"
    int           duration,
    Long          venueId,
    Long          courtId,
    boolean       moveSubsMatches,
    String        status          // "AUTO_ADVANCED" for auto-advanced BYE matches, else null
) {
    public record ParticipantRef(String id, String name, String flatNumber) {
        public ParticipantRef(String id, String name) {
            this(id, name, null);
        }
    }
}

