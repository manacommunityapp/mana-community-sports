package com.manacommunity.sports.controller;

import static com.manacommunity.sports.constants.PermissionConstants.*;
import com.manacommunity.sports.dto.SponsorDto;
import com.manacommunity.sports.dto.TournamentAnnouncementRequest;
import com.manacommunity.sports.dto.TournamentRequest;
import com.manacommunity.sports.dto.TournamentResponse;
import com.manacommunity.sports.dto.SportsEventResponse;
import com.manacommunity.sports.model.SportsEvent;
import com.manacommunity.sports.email.TournamentAnnouncementService;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.Tournament;
import com.manacommunity.sports.user.security.UserPrincipal;
import com.manacommunity.sports.user.service.LoggedInUserService;
import com.manacommunity.sports.service.SportsEventService;
import com.manacommunity.sports.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final LoggedInUserService loggedInUserService;
    private final SportsEventService eventService;
    private final TournamentAnnouncementService announcementService;
    private final com.manacommunity.sports.email.TournamentEmailService tournamentEmailService;

    @GetMapping("/announcement/{id}")
    public ResponseEntity<com.manacommunity.sports.dto.email.TournamentAnnouncementEmailDTO> announcement(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentEmailService.getTournamentAnnouncement(id));
    }

    /** Rendered HTML preview of the announcement email (open in a browser). */
    @GetMapping(value = "/announcement/{id}/preview", produces = org.springframework.http.MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> announcementPreview(@PathVariable Long id) {
        return ResponseEntity.ok(tournamentEmailService.renderAnnouncementHtml(id));
    }

    @GetMapping("/all")
    public ResponseEntity<List<TournamentResponse>> getAllTournaments(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(tournamentService.getAllTournaments().stream().map(this::toResponse).toList());
    }

    @GetMapping("/community")
    public ResponseEntity<List<TournamentResponse>> getCommunityTournaments(
            @RequestParam(required = false) Long communityId,
            @AuthenticationPrincipal UserPrincipal principal) {

        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Long targetCommunityId = communityId;
        if (!ROLE_SUPER_ADMIN.equals(loggedInUser.getRole())) {
            targetCommunityId = loggedInUser.getCommunity() != null ? loggedInUser.getCommunity().getId() : null;
        }

        if (targetCommunityId == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(tournamentService.getCommunityTournaments(targetCommunityId)
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> getTournamentById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(tournamentService.getTournamentById(id)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTournament(@PathVariable Long id) {
        tournamentService.deleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<TournamentResponse> createTournament(
            @Valid @RequestBody TournamentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser loggedInUser = loggedInUserService.resolve(principal);
        Tournament tournament = tournamentService.saveTournamentRecord(req, req.getAllowAdminChat());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(tournament));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<TournamentResponse> updateStatus(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(toResponse(tournamentService.updateStatus(id, status)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/announce")
    public ResponseEntity<Map<String, Object>> announce(
            @PathVariable Long id,
            @Valid @RequestBody TournamentAnnouncementRequest req) {
        Tournament tournament = tournamentService.getTournamentById(id);
        int sent = announcementService.announce(tournament, req);
        return ResponseEntity.ok(Map.of("sent", sent, "tournamentId", id));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SPORTS_ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/notify-open")
    public ResponseEntity<Map<String, Object>> notifyRegistrationOpen(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        Tournament tournament = tournamentService.getTournamentById(id);
        String message = body != null && body.get("message") instanceof String m ? m
                : "Registration for " + tournament.getName() + " is now open!";
        boolean sendEmail = body == null || !Boolean.FALSE.equals(body.get("sendEmail"));
        boolean sendPush = body == null || !Boolean.FALSE.equals(body.get("sendPush"));

        TournamentAnnouncementRequest req = new TournamentAnnouncementRequest(
                "TOURNAMENT_OPEN",
                "Registration is now open — " + tournament.getName(),
                message, sendEmail, sendPush, null);
        int sent = announcementService.announce(tournament, req);
        return ResponseEntity.ok(Map.of("sent", sent, "tournamentId", id));
    }

    private TournamentResponse toResponse(Tournament t) {
        List<SponsorDto> sponsorDtos = t.getSponsors() != null
                ? t.getSponsors().stream().map(s -> {
                    SponsorDto dto = new SponsorDto();
                    dto.setCategory(s.getCategory());
                    dto.setName(s.getName());
                    dto.setUrl(s.getUrl());
                    return dto;
                }).toList()
                : List.of();

        return TournamentResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .communityId(t.getCommunity() != null ? t.getCommunity().getId() : null)
                .communityName(t.getCommunity() != null ? t.getCommunity().getName() : null)
                .maxParticipants(t.getMaxParticipants())
                .description(t.getDescription())
                .eventDateStart(t.getEventDateStart())
                .eventDateEnd(t.getEventDateEnd())
                .registrationDateStart(t.getRegistrationDateStart())
                .registrationDateEnd(t.getRegistrationDateEnd())
                .startTime(t.getStartTime())
                .dueTime(t.getDueTime())
                .bannerImage(t.getBannerImage())
                .contactName(t.getContactName())
                .contactNumber(t.getContactNumber())
                .contactEmail(t.getContactEmail())
                .contacts(t.getContacts() != null
                        ? t.getContacts().stream().map(c -> com.manacommunity.sports.dto.ContactDto.builder()
                            .id(c.getId()).name(c.getName()).title(c.getTitle())
                            .number(c.getNumber()).email(c.getEmail()).build()).toList()
                        : java.util.List.of())
                .otherContacts(t.getOtherContacts())
                .allowAdminChat(t.getAllowAdminChat())
                .registrationStatus(t.getRegistrationStatus() != null ? t.getRegistrationStatus().name() : null)
                .sponsors(sponsorDtos)
                .sportsEvents(t.getSportsEvents() != null
                        ? t.getSportsEvents().stream().map(this::toEventResponse).toList()
                        : List.of())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private SportsEventResponse toEventResponse(SportsEvent e) {
        SportsEventResponse.SportRef sportRef = null;
        if (e.getSport() != null) {
            sportRef = SportsEventResponse.SportRef.builder()
                    .id(e.getSport().getId())
                    .name(e.getSport().getName())
                    .icon(e.getSport().getIcon())
                    .iconUrl(e.getSport().getIconUrl())
                    .build();
        }

        SportsEventResponse.CommunityRef communityRef = null;
        if (e.getCommunity() != null) {
            communityRef = SportsEventResponse.CommunityRef.builder()
                    .id(e.getCommunity().getId())
                    .name(e.getCommunity().getName())
                    .build();
        }

        SportsEventResponse.VenueRef venueRef = null;
        if (e.getVenue() != null) {
            venueRef = SportsEventResponse.VenueRef.builder()
                    .id(e.getVenue().getId())
                    .name(e.getVenue().getName())
                    .address(e.getVenue().getAddress())
                    .city(e.getVenue().getCity())
                    .area(e.getVenue().getArea())
                    .build();
        }

        SportsEventResponse.CreatedByRef createdByRef = null;
        if (e.getCreatedBy() != null) {
            createdByRef = SportsEventResponse.CreatedByRef.builder()
                    .id(e.getCreatedBy().getId())
                    .name(e.getCreatedBy().getFullName())
                    .email(e.getCreatedBy().getEmail())
                    .build();
        }

        List<SportsEventResponse.CategoryRef> categoryRefs = e.getCategories() != null
                ? e.getCategories().stream().map(c -> SportsEventResponse.CategoryRef.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .categoryType(c.getCategory_type())
                        .description(c.getDescription())
                        .minAge(c.getMinAge())
                        .maxAge(c.getMaxAge())
                        .gender(c.getGender())
                        .type(c.getType())
                        .build()).toList()
                : List.of();

        List<SponsorDto> sponsorDtos = e.getSponsors() != null
                ? e.getSponsors().stream().map(s -> {
                    SponsorDto dto = new SponsorDto();
                    dto.setCategory(s.getCategory());
                    dto.setName(s.getName());
                    dto.setUrl(s.getUrl());
                    return dto;
                }).toList()
                : List.of();

        SportsEventResponse.TournamentRef tournamentRef = null;
        if (e.getTournament() != null) {
            tournamentRef = SportsEventResponse.TournamentRef.builder()
                    .id(e.getTournament().getId())
                    .name(e.getTournament().getName())
                    .registrationStatus(e.getTournament().getRegistrationStatus() != null
                            ? e.getTournament().getRegistrationStatus().name() : null)
                    .build();
        }

        return SportsEventResponse.builder()
                .id(e.getId())
                .uuid(e.getUuid())
                .name(e.getName())
                .icon(e.getIcon())
                .minAge(e.getMinAge())
                .maxAge(e.getMaxAge())
                .minPlayers(e.getMinPlayers())
                .maxPlayers(e.getMaxPlayers())
                .gender(e.getGender())
                .playersBorn(e.getPlayersBorn())
                .active(e.getActive())
                .adminApprovalRequired(e.getAdminApprovalRequired())
                .sport(sportRef)
                .community(communityRef)
                .venue(venueRef)
                .createdBy(createdByRef)
                .eventDateStart(e.getEventDateStart() != null ? e.getEventDateStart() : (e.getTournament() != null ? e.getTournament().getEventDateStart() : null))
                .eventDateEnd(e.getEventDateEnd() != null ? e.getEventDateEnd() : (e.getTournament() != null ? e.getTournament().getEventDateEnd() : null))
                .registrationDateStart(e.getRegistrationDateStart() != null ? e.getRegistrationDateStart() : (e.getTournament() != null ? e.getTournament().getRegistrationDateStart() : null))
                .registrationDateEnd(e.getRegistrationDateEnd() != null ? e.getRegistrationDateEnd() : (e.getTournament() != null ? e.getTournament().getRegistrationDateEnd() : null))
                .maxParticipants(e.getMaxParticipants() != null ? e.getMaxParticipants() : (e.getTournament() != null ? e.getTournament().getMaxParticipants() : null))
                .startTime(e.getStartTime() != null ? e.getStartTime() : (e.getTournament() != null ? e.getTournament().getStartTime() : null))
                .dueTime(e.getDueTime() != null ? e.getDueTime() : (e.getTournament() != null ? e.getTournament().getDueTime() : null))
                .status(e.getStatus() != null ? e.getStatus().name() : null)
                .auctionStatus(e.getAuctionStatus() != null ? e.getAuctionStatus().name() : null)
                .format(e.getFormat())
                .tournamentType(e.getTournamentType() != null ? e.getTournamentType().name() : null)
                .categories(categoryRefs)
                .sponsors(sponsorDtos)
                .contactName(e.getContactName() != null ? e.getContactName() : (e.getTournament() != null ? e.getTournament().getContactName() : null))
                .contactNumber(e.getContactNumber() != null ? e.getContactNumber() : (e.getTournament() != null ? e.getTournament().getContactNumber() : null))
                .contactEmail(e.getContactEmail() != null ? e.getContactEmail() : (e.getTournament() != null ? e.getTournament().getContactEmail() : null))
                .contacts(e.getContacts() != null
                        ? e.getContacts().stream().map(c -> com.manacommunity.sports.dto.ContactDto.builder()
                            .id(c.getId()).name(c.getName()).title(c.getTitle())
                            .number(c.getNumber()).email(c.getEmail()).build()).toList()
                        : java.util.List.of())
                .otherContacts(e.getOtherContacts() != null ? e.getOtherContacts() : (e.getTournament() != null ? e.getTournament().getOtherContacts() : null))
                .auctionEnabled(e.getAuctionEnabled())
                .bannerImage(e.getBannerImage() != null ? e.getBannerImage() : (e.getTournament() != null ? e.getTournament().getBannerImage() : null))
                .tournamentLevel(e.getTournamentLevel())
                .description(e.getDescription() != null ? e.getDescription() : (e.getTournament() != null ? e.getTournament().getDescription() : null))
                .disputeCommitteeIds(e.getDisputeCommittee() != null
                        ? e.getDisputeCommittee().stream().map(u -> u.getId()).toList()
                        : List.of())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
