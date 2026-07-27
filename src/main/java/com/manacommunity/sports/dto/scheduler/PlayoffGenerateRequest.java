package com.manacommunity.sports.dto.scheduler;

import java.util.List;

/**
 * Stateless input for generating a playoff ("rounds to final") bracket.
 * Mirrors the UI's PlayoffScheduleInput (playoffSchedule.ts) so the bracket
 * the server produces is identical to what the browser used to compute.
 *
 * <p>When {@link #participants} is provided (single-elimination knockout from
 * registered players), the generator uses the player-aware path: BYE
 * auto-advance, flat/tower-aware first-round pairing, and parallel court
 * allocation across {@link #courtIds}. Otherwise it uses the slot-based path.
 */
public record PlayoffGenerateRequest(
    int     numGroups,
    int     proceedersPerGroup,
    String  seedingOrder,           // "TRADITIONAL" | "SEQUENTIAL" | "RANDOM"
    boolean thirdPlaceMatch,
    String  startDate,              // "yyyy-MM-dd"
    String  startTime,              // 12-hour "hh:mm AM/PM"
    int     matchDurationMinutes,
    int     breakMinutes,
    Long    venueId,
    Long    courtId,
    List<ParticipantInput> participants,  // seeded registered players (knockout-only); null/empty => slot path
    List<Long>             courtIds        // selected courts for parallel allocation (Rule 4)
) {
    /**
     * A seeded player participating directly in a knockout draw.
     * {@code rating} is the optional skill rating used for balanced pairing
     * (Rule 6); null when the sport/registration carries no rating.
     */
    public record ParticipantInput(String id, String name, String flatNumber, Integer rating) {
        /** Back-compat constructor for callers without a skill rating. */
        public ParticipantInput(String id, String name, String flatNumber) {
            this(id, name, flatNumber, null);
        }
    }
}
