package com.manacommunity.sports.dto.schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lean DTOs for the Sports Schedule page. Returns only fields
 * the UI actually renders in list views, instead of the full
 * 43-field SportsEventResponse.
 */
public final class SportsScheduleResponse {

    private SportsScheduleResponse() {}

    /**
     * Minimal event card for list views (schedule, my events, open events).
     * ~14 fields vs 43+ in SportsEventResponse.
     */
    public record EventListItem(
            Long id,
            String uuid,
            String name,
            String sportName,
            String venueName,
            String venueCity,
            LocalDate eventDateStart,
            LocalDate eventDateEnd,
            String registrationStatus,
            String auctionStatus,
            List<String> format,
            String categoryName,
            Integer maxParticipants,
            String startTime
    ) {}

    /**
     * Minimal registration row for "my registrations" view.
     */
    public record RegistrationListItem(
            Long id,
            String status,
            Long eventId,
            String eventName,
            String sportName,
            String categoryName,
            Integer age,
            String flatNumber,
            LocalDateTime registeredAt,
            Boolean captainNomination,
            Boolean captainConfirmation,
            String proposedTeamName
    ) {}
}
