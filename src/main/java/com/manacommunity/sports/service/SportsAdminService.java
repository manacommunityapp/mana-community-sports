package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import static com.manacommunity.sports.constants.PermissionConstants.*;
import com.manacommunity.sports.dto.dashboard.SportsAdminFormDataResponse;
import com.manacommunity.sports.dto.dashboard.SportsAdminFormDataResponse.CategoryOption;
import com.manacommunity.sports.dto.dashboard.SportsAdminFormDataResponse.SportOption;
import com.manacommunity.sports.dto.dashboard.SportsAdminOverviewResponse;
import com.manacommunity.sports.dto.dashboard.SportsAdminOverviewResponse.EventRow;
import com.manacommunity.sports.dto.dashboard.SportsAdminOverviewResponse.SportRef;
import com.manacommunity.sports.dto.dashboard.SportsAdminOverviewResponse.TournamentRow;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.sports.model.PlayerCategory;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.model.SportsMeta;
import com.manacommunity.sports.model.Tournament;
import com.manacommunity.sports.repository.PlayerCategoryRepository;
import com.manacommunity.sports.repository.SportMetaRepository;
import com.manacommunity.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.sports.model.SportsEventRegistration;
import com.manacommunity.sports.dto.dashboard.SportsAdminOverviewResponse.RegistrationRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates the Sports Admin reads into lean, purpose-built payloads — the list
 * tabs and the create forms each get exactly the fields they bind to, in one call,
 * instead of several full-entity fetches. Mirrors the scoping rules of
 * {@code SportsController} (SUPER_ADMIN sees all; others see their community).
 */
@Service
@RequiredArgsConstructor
public class SportsAdminService {

    private final TournamentService tournamentService;
    private final SportsEventService eventService;
    private final com.manacommunity.sports.repository.CommunityRepository communityRepo;
    private final SportMetaRepository sportMetaRepo;
    private final PlayerCategoryRepository categoryRepo;
    private final SportsEventRegistrationRepository regRepo;

    // ── Overview (Dashboard / Sports Event tabs) ──────────────────────

    @Transactional(readOnly = true)
    public SportsAdminOverviewResponse getOverview(AppUser user, Long communityId) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long activeCommId = isSuperAdmin ? communityId : (user.getCommunity() != null ? user.getCommunity().getId() : null);

        List<Tournament> tournaments = isSuperAdmin && activeCommId == null
                ? tournamentService.getAllTournaments()
                : (activeCommId != null ? tournamentService.getCommunityTournaments(activeCommId) : List.of());

        PageRequest eventPage = PageRequest.of(0, 100, Sort.by("eventDateStart").descending());
        List<SportsEvent> events = isSuperAdmin && activeCommId == null
                ? eventService.getAllEvents(eventPage).getContent()
                : (activeCommId != null ? eventService.getCommunityEvents(activeCommId, eventPage).getContent() : List.of());

        List<TournamentRow> tournamentRows = tournaments.stream()
                .map(this::toTournamentRow)
                .toList();

        List<EventRow> eventRows = events.stream()
                .map(this::toEventRow)
                .toList();

        List<SportsEventRegistration> registrations;
        if (isSuperAdmin && activeCommId == null) {
            registrations = regRepo.findAll();
        } else if (activeCommId != null) {
            registrations = regRepo.findByCommunityId(activeCommId);
        } else {
            registrations = List.of();
        }

        List<RegistrationRow> pendingRegistrations = registrations.stream()
                .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.PENDING 
                        || r.getStatus() == SportsEventRegistration.RegistrationStatus.REGISTERED)
                .map(this::toRegistrationRow)
                .toList();

        List<RegistrationRow> confirmedRegistrations = registrations.stream()
                .filter(r -> r.getStatus() == SportsEventRegistration.RegistrationStatus.CONFIRMED)
                .map(this::toRegistrationRow)
                .toList();

        return new SportsAdminOverviewResponse(tournamentRows, eventRows, pendingRegistrations, confirmedRegistrations);
    }

    private RegistrationRow toRegistrationRow(SportsEventRegistration r) {
        String eventName = r.getEvent() != null ? r.getEvent().getName() : "";
        String sportName = r.getEvent() != null && r.getEvent().getSport() != null ? r.getEvent().getSport().getName() : "";
        return new RegistrationRow(
                r.getId(),
                r.getPlayerName(),
                r.getEmail(),
                r.getFlatNumber(),
                r.getRole(),
                r.getRelation(),
                r.getAge(),
                r.getStatus() != null ? r.getStatus().name() : null,
                eventName,
                sportName,
                r.getRegisteredAt(),
                r.getProposedTeamName()
        );
    }

    private TournamentRow toTournamentRow(Tournament t) {
        List<EventRow> nested = t.getSportsEvents() == null ? List.of()
                : t.getSportsEvents().stream().map(this::toEventRow).toList();
        return new TournamentRow(
                t.getId(),
                t.getName(),
                t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null,
                t.getMaxParticipants(),
                t.getEventDateStart(),
                t.getEventDateEnd(),
                nested
        );
    }

    private EventRow toEventRow(SportsEvent e) {
        SportsMeta sport = e.getSport();
        Tournament t = e.getTournament();
        return new EventRow(
                e.getId(),
                e.getName(),
                e.getEventDateStart(),
                e.getEventDateEnd(),
                e.getFormat(),
                e.getTournamentType() != null ? e.getTournamentType().name() : null,
                e.getGender(),
                e.getMinAge(),
                e.getMaxAge(),
                e.getMaxParticipants(),
                t != null && t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null,
                e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null,
                sport != null ? new SportRef(sport.getName(), sport.getIcon(), sport.getIconUrl()) : null,
                t != null ? t.getId() : null,
                e.getAdminApprovalRequired()
        );
    }

    // ── Form data (Create Tournament / Create Venue tabs) ─────────────

    @Transactional(readOnly = true)
    public SportsAdminFormDataResponse getFormData(AppUser user) {
        boolean isSuperAdmin = ROLE_SUPER_ADMIN.equals(user.getRole());
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;

        List<SportOption> sports = sportMetaRepo.findByActiveTrue().stream()
                .map(s -> new SportOption(
                        s.getId(), s.getName(), s.getIcon(), s.getIconUrl(),
                        s.getFormats(), s.getCommunityId()))
                .toList();

        // Same visibility rule as SportsController#getCategories.
        List<PlayerCategory> rawCategories = (isSuperAdmin || communityId == null)
                ? categoryRepo.findAll()
                : categoryRepo.findDefaultAndCommunityCategories(communityId);

        List<CategoryOption> categories = rawCategories.stream()
                .map(c -> new CategoryOption(
                        c.getId(), c.getName(), c.getType(), c.getCategory_type(),
                        c.getDescription(), c.getMinAge(), c.getMaxAge(), c.getGender()))
                .toList();

        List<com.manacommunity.sports.response.CommunityResponse> communityResponses = communityRepo.findAll().stream()
                .map(c -> new com.manacommunity.sports.response.CommunityResponse(
                        c.getId(), c.getName(), c.getType(), c.getCity(), c.getState(),
                        c.getArea(), c.getSubtype(), c.getInviteCode(), c.getActive(), java.util.List.of()))
                .toList();

        return new SportsAdminFormDataResponse(
                sports, categories, communityResponses);
    }
}



