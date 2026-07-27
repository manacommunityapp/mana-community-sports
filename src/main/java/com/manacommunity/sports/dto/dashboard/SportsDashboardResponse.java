package com.manacommunity.sports.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * Lean, purpose-built payload for the Sports Dashboard page. Replaces the four
 * separate "fat" calls the page used to make (open events, closed events,
 * my events, my registrations) — each of which serialised the full
 * {@code SportsEvent} graph — with a single response carrying only the fields
 * the dashboard actually renders.
 */
public record SportsDashboardResponse(
        Stats stats,
        List<EventCard> openRegistrations,
        List<EventCard> closedRegistrations,
        List<UpcomingEvent> myUpcomingEvents,
        List<MyRegistration> myRegistrations,
        List<TournamentCard> openTournaments
) {

    /** The four headline stat cards. */
    public record Stats(
            int yourRegistrations,
            int liveEvents,
            int openRegistrations,
            int upcomingTournaments
    ) {}

    /** A row in "Open for Registration" / "Closed Registrations". */
    public record EventCard(
            Long id,
            java.util.UUID uuid,
            String name,
            LocalDate eventDateStart,
            LocalDate eventDateEnd,
            String sportName,
            String categoryName,
            String venueName,
            Integer maxParticipants,
            String registrationStatus,
            String auctionStatus,
            boolean teamSport,
            // The current user's registration for this event (if any) — drives the button state.
            Long myRegistrationId,
            String myRegistrationStatus
    ) {}

    /** A tournament with its child events grouped for "Open for Registration". */
    public record TournamentCard(
            Long id,
            String name,
            String bannerImage,
            LocalDate eventDateStart,
            LocalDate eventDateEnd,
            String registrationStatus,
            Long communityId,
            String communityName,
            List<EventCard> events
    ) {}

    /** A row in "Your Upcoming Events" (also feeds the next-match timer & notifications). */
    public record UpcomingEvent(
            Long id,
            String name,
            String sportName,
            String venueName,
            String categoryName,
            String registrationStatus,
            LocalDate eventDateStart,
            String startTime,
            Long tournamentId,
            String tournamentName
    ) {}

    /** The current user's registrations — used for button states and captaincy. */
    public record MyRegistration(
            Long id,
            Long eventId,
            String eventName,
            LocalDate eventDateStart,
            String sportName,
            String categoryName,
            String eventRegistrationStatus,
            String status,
            String matchType,
            Boolean captainNomination,
            Boolean captainConfirmation
    ) {}
}
