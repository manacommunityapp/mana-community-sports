package com.manacommunity.sports.service.impl;

import com.manacommunity.common.enums.*;
import com.manacommunity.sports.dto.TournamentRequest;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.TournamentRepository;
import com.manacommunity.sports.repository.SportsEventRepository;
import com.manacommunity.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.sports.repository.CommunityRepository;
import com.manacommunity.sports.repository.ContactRepository;
import com.manacommunity.sports.dto.ContactDto;
import com.manacommunity.sports.service.NotificationManagementService;
import com.manacommunity.sports.service.TournamentService;
import com.manacommunity.sports.email.TournamentEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepo;
    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final CommunityRepository communityRepo;
    private final ContactRepository contactRepository;
    private final NotificationManagementService notificationService;
    private final TournamentEmailService tournamentEmailService;

    private List<Contact> resolveContacts(List<ContactDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        return dtos.stream().map(dto -> {
            if (dto.getId() != null) {
                return contactRepository.findById(dto.getId())
                        .orElseGet(() -> contactRepository.save(Contact.builder()
                                .name(dto.getName()).title(dto.getTitle())
                                .number(dto.getNumber()).email(dto.getEmail()).build()));
            }
            return contactRepository.findByNameAndNumberAndEmail(dto.getName(), dto.getNumber(), dto.getEmail())
                    .orElseGet(() -> contactRepository.save(Contact.builder()
                            .name(dto.getName()).title(dto.getTitle())
                            .number(dto.getNumber()).email(dto.getEmail()).build()));
        }).collect(Collectors.toList());
    }

    @Override
    public List<Tournament> getAllTournaments() {
        return tournamentRepo.findAll();
    }

    @Override
    public List<Tournament> getCommunityTournaments(Long communityId) {
        return tournamentRepo.findByEventCommunityIdOrderByCreatedAtDesc(communityId);
    }

    @Override
    public Tournament getTournamentById(Long id) {
        return tournamentRepo.findById(id)
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("Tournament", id));
    }

    @Override
    @Transactional
    public void deleteTournament(Long id) {
        Tournament tournament = tournamentRepo.findById(id).orElse(null);
        if (tournament == null) {
            return; // already gone — treat delete as idempotent
        }

        // Disassociate the sports events instead of deleting them: null out the
        // FK on each event so the tournament can be removed without cascade-
        // deleting events (which own registrations/matches and would block the
        // delete). The events survive, just unlinked from any tournament.
        List<SportsEvent> events = eventRepo.findByTournamentId(id);
        for (SportsEvent ev : events) {
            ev.setTournament(null);
        }
        eventRepo.saveAll(events);

        // Keep the in-memory collection consistent so JPA doesn't re-link on flush.
        if (tournament.getSportsEvents() != null) {
            tournament.getSportsEvents().clear();
        }

        tournamentRepo.delete(tournament);
    }

    @Override
    @Transactional
    public Tournament saveTournamentRecord(TournamentRequest req, Boolean allowAdminChat) {
        // Create/resolve path: reuse an existing Tournament only if one is already
        // linked to a selected event, otherwise start a fresh record.
        Tournament tournament = null;
        if (req.getSportsEventIds() != null && !req.getSportsEventIds().isEmpty()) {
            for (Long eventId : req.getSportsEventIds()) {
                Optional<Tournament> existing = tournamentRepo.findByEventId(eventId);
                if (existing.isPresent()) {
                    tournament = existing.get();
                    break;
                }
            }
        }
        if (tournament == null) {
            tournament = new Tournament();
        }
        return applyTournamentFields(tournament, req, allowAdminChat);
    }

    @Override
    @Transactional
    public Tournament updateTournamentRecord(Long id, TournamentRequest req, Boolean allowAdminChat) {
        // Update path: the tournament is identified authoritatively by its id (with a
        // fallback lookup by linked event id for older callers). We NEVER create a new
        // Tournament here — doing so was the cause of tournaments duplicating on edit.
        Tournament tournament = tournamentRepo.findById(id)
                .or(() -> tournamentRepo.findByEventId(id))
                .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException("Tournament", id));
        return applyTournamentFields(tournament, req, allowAdminChat);
    }

    private Tournament applyTournamentFields(Tournament tournament, TournamentRequest req, Boolean allowAdminChat) {
        boolean isNew = tournament.getId() == null;

        // Map tournament fields directly from TournamentRequest DTO
        tournament.setName(req.getName());
        tournament.setEventDateStart(req.getEventDateStart());
        tournament.setEventDateEnd(req.getEventDateEnd());
        tournament.setRegistrationDateStart(req.getRegistrationDateStart());
        tournament.setRegistrationDateEnd(req.getRegistrationDateEnd());
        tournament.setMaxParticipants(req.getMaxParticipants());
        
        tournament.setContactName(req.getContactName());
        tournament.setContactNumber(req.getContactNumber());
        tournament.setContactEmail(req.getContactEmail());
        tournament.getContacts().clear();
        tournament.getContacts().addAll(resolveContacts(req.getContacts()));
        tournament.setOtherContacts(req.getOtherContacts());
        
        tournament.setBannerImage(req.getBannerImage());
        tournament.setDescription(req.getDescription());
        tournament.setAllowAdminChat(allowAdminChat);
        
        tournament.setStartTime(req.getStartTime());
        tournament.setDueTime(req.getDueTime());

        if (req.getCommunityId() != null) {
            tournament.setCommunity(communityRepo.findById(req.getCommunityId()).orElse(null));
        }

        // Resolve existing SportsEvents from req.getSportsEventIds()
        List<SportsEvent> sportsEvents = new ArrayList<>();
        if (req.getSportsEventIds() != null && !req.getSportsEventIds().isEmpty()) {
            sportsEvents = eventRepo.findAllById(req.getSportsEventIds());
        }

        // Unlink events that are no longer associated
        if (tournament.getSportsEvents() != null) {
            for (SportsEvent oldEvent : new ArrayList<>(tournament.getSportsEvents())) {
                if (!sportsEvents.contains(oldEvent)) {
                    oldEvent.setTournament(null);
                    eventRepo.save(oldEvent);
                }
            }
        }

        // Link new/current events & sync tournament-level fields
        tournament.setSportsEvents(new ArrayList<>());
        for (SportsEvent ev : sportsEvents) {
            ev.setTournament(tournament);
            if (req.getEventDateStart() != null) ev.setEventDateStart(req.getEventDateStart());
            if (req.getEventDateEnd() != null) ev.setEventDateEnd(req.getEventDateEnd());
            if (req.getRegistrationDateStart() != null) ev.setRegistrationDateStart(req.getRegistrationDateStart());
            if (req.getRegistrationDateEnd() != null) ev.setRegistrationDateEnd(req.getRegistrationDateEnd());
            if (req.getMaxParticipants() != null) ev.setMaxParticipants(req.getMaxParticipants());
            if (req.getDescription() != null) ev.setDescription(req.getDescription());
            if (req.getStartTime() != null) ev.setStartTime(req.getStartTime());
            if (req.getDueTime() != null) ev.setDueTime(req.getDueTime());
            if (req.getBannerImage() != null) ev.setBannerImage(req.getBannerImage());
            if (req.getContactName() != null) ev.setContactName(req.getContactName());
            if (req.getContactNumber() != null) ev.setContactNumber(req.getContactNumber());
            if (req.getContactEmail() != null) ev.setContactEmail(req.getContactEmail());
            if (req.getOtherContacts() != null) ev.setOtherContacts(req.getOtherContacts());
            if (req.getTournamentLevel() != null) ev.setTournamentLevel(req.getTournamentLevel());
            tournament.getSportsEvents().add(ev);
            eventRepo.save(ev);
        }
        // Only default to DRAFT for brand-new tournaments; editing must not demote
        // an already-open/completed tournament back to DRAFT.
        if (isNew || tournament.getRegistrationStatus() == null) {
            tournament.setRegistrationStatus(Tournament.EventStatus.DRAFT);
        }

        // Build mainEvent context for sponsors mapping
        SportsEvent mainEvent = null;
        if (!sportsEvents.isEmpty()) {
            mainEvent = sportsEvents.get(0);
        }

        // Clear and rebuild sponsors directly from request DTO
        if (tournament.getSponsors() != null) {
            tournament.getSponsors().clear();
        } else {
            tournament.setSponsors(new ArrayList<>());
        }

        if (req.getSponsors() != null) {
            for (com.manacommunity.sports.dto.SponsorDto s : req.getSponsors()) {
                tournament.getSponsors().add(SportsEventSponsor.builder()
                        .tournament(tournament)
                        .event(mainEvent)
                        .category(s.getCategory())
                        .name(s.getName())
                        .url(s.getUrl())
                        .build());
            }
        }

        if (tournament.getCreatedAt() == null) {
            tournament.setCreatedAt(java.time.LocalDateTime.now());
        }
        tournament.setUpdatedAt(java.time.LocalDateTime.now());

        return tournamentRepo.save(tournament);
    }

    @Override
    @Transactional
    public Tournament updateStatus(Long id, String status) {
        Tournament.EventStatus tournamentStatus;
        try {
            tournamentStatus = Tournament.EventStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new com.manacommunity.common.exception.ManaCommunityException(
                    "Invalid event status: '" + status + "'. Valid values: DRAFT, REGISTRATION_OPEN, "
                    + "REGISTRATION_CLOSED, LIVE, COMPLETED, CANCELLED.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }

        Tournament tournament = tournamentRepo.findById(id).orElse(null);
        if (tournament == null) {
            SportsEvent event = eventRepo.findById(id)
                    .orElseThrow(() -> new com.manacommunity.common.exception.ResourceNotFoundException(
                            "Tournament or Event", id));
            tournament = event.getTournament();
            if (tournament == null) {
                throw new com.manacommunity.common.exception.ManaCommunityException(
                        "Event " + id + " is not linked to a tournament.",
                        org.springframework.http.HttpStatus.BAD_REQUEST, "NO_TOURNAMENT");
            }
        }

        tournament.setRegistrationStatus(tournamentStatus);
        Tournament saved = tournamentRepo.save(tournament);

        if (tournamentStatus == Tournament.EventStatus.COMPLETED
                || tournamentStatus == Tournament.EventStatus.CANCELLED) {
            if (saved.getSportsEvents() != null) {
                for (SportsEvent ev : saved.getSportsEvents()) {
                    ev.setActive(false);
                    ev.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
                    ev.setUpdatedAt(java.time.LocalDateTime.now());
                    eventRepo.save(ev);
                }
            }
        } else {
            if (saved.getSportsEvents() != null) {
                for (SportsEvent ev : saved.getSportsEvents()) {
                    ev.setActive(true);
                    ev.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
                    ev.setUpdatedAt(java.time.LocalDateTime.now());
                    eventRepo.save(ev);
                }
            }
        }

        notifyTournamentParticipants(saved, tournamentStatus);

        if (tournamentStatus == Tournament.EventStatus.REGISTRATION_OPEN) {
            try {
                tournamentEmailService.sendTournamentRegistrationOpen(saved);
            } catch (Exception e) {
                log.error("Failed to send tournament registration open email for tournament {}: {}", saved.getId(), e.getMessage());
            }
        }

        return saved;
    }

    private void notifyTournamentParticipants(Tournament tournament, Tournament.EventStatus newStatus) {
        try {
            if (tournament.getSportsEvents() == null || tournament.getSportsEvents().isEmpty()) return;

            List<Long> userIds = tournament.getSportsEvents().stream()
                    .flatMap(ev -> regRepo.findByEventId(ev.getId()).stream())
                    .filter(r -> r.getUser() != null && r.getUser().getId() != null)
                    .map(r -> r.getUser().getId())
                    .distinct()
                    .toList();
            if (userIds.isEmpty()) return;

            NotificationType type;
            String title;
            String body;
            NotificationPriority priority;

            switch (newStatus) {
                case REGISTRATION_OPEN -> {
                    type = NotificationType.REGISTRATION_OPEN;
                    title = "Registrations Open — " + tournament.getName();
                    body = "Registrations are now open for " + tournament.getName();
                    priority = NotificationPriority.HIGH;
                }
                case CANCELLED -> {
                    type = NotificationType.EVENT_CANCELLED;
                    title = "Tournament Cancelled — " + tournament.getName();
                    body = "This tournament has been cancelled";
                    priority = NotificationPriority.HIGH;
                }
                case COMPLETED -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = "Tournament Completed — " + tournament.getName();
                    body = "The tournament has concluded. Thank you for participating!";
                    priority = NotificationPriority.NORMAL;
                }
                default -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = tournament.getName() + " — Status Update";
                    body = "Status changed to " + newStatus.name().replace('_', ' ').toLowerCase();
                    priority = NotificationPriority.NORMAL;
                }
            }

            notificationService.createBulkNotifications(
                    userIds, type, NotificationCategory.EVENTS,
                    title, body, null,
                    ReferenceType.TOURNAMENT, tournament.getId(),
                    priority, null,
                    tournament.getCommunity() != null ? tournament.getCommunity().getId() : null);
        } catch (Exception e) {
            log.warn("Failed to persist tournament-status notifications for tournament {}: {}",
                    tournament.getId(), e.getMessage());
        }
    }
}



