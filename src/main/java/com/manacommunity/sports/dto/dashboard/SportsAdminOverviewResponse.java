package com.manacommunity.sports.dto.dashboard;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Role;
import java.time.LocalDate;
import java.util.List;

/**
 * Lean payload for the Sports Admin "Dashboard" / "Sports Event" tabs. Replaces the
 * separate {@code getAllTournaments|getCommunityTournaments} + {@code getAllEvents|
 * getCommunityEvents} calls (each serialising the full entity graph) with one
 * response carrying only the columns the admin lists render.
 */
public record SportsAdminOverviewResponse(
        List<TournamentRow> tournaments,
        List<EventRow> events,
        List<RegistrationRow> pendingRegistrations,
        List<RegistrationRow> confirmedRegistrations
) {

    public record RegistrationRow(
            Long id,
            String playerName,
            String email,
            String flatNumber,
            String role,
            String relation,
            Integer age,
            String status,
            String eventName,
            String sportName,
            java.time.LocalDateTime registeredAt,
            String proposedTeamName
    ) {}

    /** A row in the tournaments list, with its nested events. */
    public record TournamentRow(
            Long id,
            String name,
            String registrationStatus,
            Integer maxParticipants,
            LocalDate eventDateStart,
            LocalDate eventDateEnd,
            List<EventRow> events
    ) {}

    /** A row in the events list (also nested under a tournament). */
    public record EventRow(
            Long id,
            String name,
            LocalDate eventDateStart,
            LocalDate eventDateEnd,
            List<String> format,
            String tournamentType,
            String gender,
            Integer minAge,
            Integer maxAge,
            Integer maxParticipants,
            String registrationStatus,
            String auctionStatus,
            SportRef sport,
            Long tournamentId,
            Boolean adminApprovalRequired
    ) {}

    /** Just the sport fields the cards display (name + icon/iconUrl). */
    public record SportRef(String name, String icon, String iconUrl) {}
}


