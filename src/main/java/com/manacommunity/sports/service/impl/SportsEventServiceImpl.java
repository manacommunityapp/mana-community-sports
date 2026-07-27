package com.manacommunity.sports.service.impl;

import com.manacommunity.sports.model.Community;

import com.manacommunity.sports.user.repository.AppUserRepository;

import com.manacommunity.sports.user.model.AppUser;

import com.manacommunity.sports.dto.SportsEventRequest;
import com.manacommunity.sports.dto.NotificationScheduleDto;
import com.manacommunity.sports.dto.RegistrationRequest;
import com.manacommunity.sports.dto.SponsorDto;
import com.manacommunity.sports.exception.*;
import com.manacommunity.sports.email.RegistrationEmailService;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.repository.*;
import com.manacommunity.sports.service.NotificationManagementService;
import com.manacommunity.sports.service.SportsEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

import com.manacommunity.sports.repository.ContactRepository;
import com.manacommunity.sports.model.Contact;
import com.manacommunity.sports.dto.ContactDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class SportsEventServiceImpl implements SportsEventService {

    private final SportsEventRepository eventRepo;
    private final SportsEventRegistrationRepository regRepo;
    private final SportMetaRepository sportMetaRepo;
    private final PlayerCategoryRepository categoryRepo;
    private final SportsNotificationSchedulerRepository schedulerRepo;
    private final AppUserRepository userRepo;
    private final CommunityRepository communityRepo;
    private final VenueRepository venueRepo;
    private final AuctionConfigRepository auctionConfigRepo;
    private final AuctionTeamRepository auctionTeamRepo;
    private final AuctionPlayerRepository playerRepo;
    private final TournamentRepository tournamentRepo;
    private final RegistrationEmailService registrationEmailService;
    private final NotificationManagementService notificationService;
    private final com.manacommunity.sports.service.RecaptchaService recaptchaService;
    private final com.manacommunity.sports.service.OtpService otpService;
    private final ContactRepository contactRepository;

    private java.util.List<Contact> resolveContacts(java.util.List<ContactDto> dtos) {
        if (dtos == null || dtos.isEmpty()) return new java.util.ArrayList<>();
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
        }).collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public SportsEvent createEvent(SportsEventRequest req, Long adminUserId) {
        SportsMeta sport = sportMetaRepo.findById(req.getSportId())
                .orElseThrow(() -> new ResourceNotFoundException("Sport", req.getSportId()));

        Venue venue = null;
        if (req.getVenueId() != null) {
            venue = venueRepo.findById(req.getVenueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId()));
        }

        SportsEvent event = SportsEvent.builder()
                .name(req.getName())
                .active(true)
                .sport(sport)
                .community(communityRepo.findById(req.getCommunityId()).orElseThrow(() -> new ResourceNotFoundException("Community", req.getCommunityId())))
                .eventDateStart(req.getEventDateStart())
                .eventDateEnd(req.getEventDateEnd())
                .registrationDateStart(req.getRegistrationDateStart())
                .registrationDateEnd(req.getRegistrationDateEnd())
                .venue(venue)
                .maxParticipants(req.getMaxParticipants() != null ? req.getMaxParticipants() : 64)
                .status(SportsEvent.EventStatus.DRAFT)
                .format(req.getFormat() != null ? java.util.Arrays.asList(req.getFormat().split(",")) : new java.util.ArrayList<>())
                .tournamentType(req.getTournamentType() != null
                        ? SportsEvent.TournamentType.valueOf(req.getTournamentType()) : null)
                .createdBy(userRepo.getReferenceById(adminUserId))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .disputeCommittee(resolveDisputeCommittee(req.getDisputeCommitteeIds()))
                .minPlayers(req.getMinPlayers())
                .maxPlayers(req.getMaxPlayers())
                .gender(req.getGender())
                .playersBorn(req.getPlayersBorn())
                .contactName(req.getContactName())
                .contactNumber(req.getContactNumber())
                .contactEmail(req.getContactEmail())
                .contacts(resolveContacts(req.getContacts()))
                .otherContacts(req.getOtherContacts())
                .auctionEnabled(req.getAuctionEnabled() != null && req.getAuctionEnabled())
                .bannerImage(req.getBannerImage())
                .tournamentLevel(req.getTournamentLevel())
                .description(req.getDescription())
                .startTime(req.getStartTime())
                .dueTime(req.getDueTime())
                .minAge(req.getMinAge() != null ? req.getMinAge() : 0)
                .maxAge(req.getMaxAge() != null ? req.getMaxAge() : 100)
                .adminApprovalRequired(req.getAdminApprovalRequired() == null || req.getAdminApprovalRequired())
                .build();

        if (req.getCategoryIds() != null)
            event.setCategories(new java.util.HashSet<>(categoryRepo.findAllById(req.getCategoryIds())));

        if (req.getSponsors() != null) {
            List<SportsEventSponsor> sponsorsList = new java.util.ArrayList<>();
            for (SponsorDto s : req.getSponsors()) {
                sponsorsList.add(SportsEventSponsor.builder()
                        .event(event)
                        .category(s.getCategory())
                        .name(s.getName())
                        .url(s.getUrl())
                        .build());
            }
            event.setSponsors(sponsorsList);
        }

        if (req.getTournamentId() != null) {
            Tournament tournament = tournamentRepo.findById(req.getTournamentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tournament", req.getTournamentId()));
            event.setTournament(tournament);
        }

        SportsEvent saved = eventRepo.save(event);

        if (req.getNotifications() != null)
            scheduleNotifications(saved, req.getNotifications());

        return saved;
    }

    @Transactional
    public SportsEventRegistration registerUser(RegistrationRequest req, Long userId) {
        SportsEvent event = eventRepo.findById(req.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", req.getEventId()));

        if (event.getStatus() != SportsEvent.EventStatus.REGISTRATION_OPEN)
            throw new RegistrationClosedException(event.getName(), event.getStatus().name());

        // Anti-abuse gates (both no-ops unless enabled in config): bot check then
        // proof the registrant controls the email they're submitting.
        recaptchaService.verify(req.getRecaptchaToken(), null);
        otpService.assertEmailVerified(req.getEmail());

        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Allow multiple registrations if playerName is different (for family members)
        String pName = req.getPlayerName() != null ? req.getPlayerName() : user.getFullName();
        String email = req.getEmail();
        String flat = req.getFlatNumber();
        // Duplicate guard: a registration is a duplicate when name + email + flat number all match
        // an existing one for this event (case-insensitive, trimmed) — regardless of which user
        // submitted it, so admin-imported and self-registered duplicates are both caught.
        boolean duplicate = regRepo.findByEventId(req.getEventId()).stream().anyMatch(r ->
                normEq(r.getPlayerName(), pName)
                        && normEq(r.getEmail(), email)
                        && normEq(r.getFlatNumber(), flat));
        if (duplicate) {
            throw new AlreadyRegisteredException(
                    "Registration for " + pName
                            + " (" + (email != null && !email.isBlank() ? email : "no email") + ", "
                            + (flat != null && !flat.isBlank() ? flat : "no flat") + ") already exists.");
        }

        long currentCount = regRepo.countByEventId(req.getEventId());
        if (currentCount >= event.getMaxParticipants())
            throw new EventFullException(event.getName(), event.getMaxParticipants());

        PlayerCategory category = categoryRepo.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("PlayerCategory", req.getCategoryId()));

        int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
        int minAge = event.getMinAge() != null ? event.getMinAge() : 0;
        int maxAge = event.getMaxAge() != null ? event.getMaxAge() : 100;
        if (age < minAge || age > maxAge)
            throw new AgeMismatchException(age, minAge, maxAge, event.getSport().getName());

        // Admin-approval toggle: when the event requires vetting, the entry lands
        // PENDING and an organiser must confirm it; otherwise it auto-confirms.
        boolean approvalRequired = event.getAdminApprovalRequired() == null || event.getAdminApprovalRequired();
        SportsEventRegistration.RegistrationStatus initialStatus = approvalRequired
                ? SportsEventRegistration.RegistrationStatus.PENDING
                : SportsEventRegistration.RegistrationStatus.CONFIRMED;

        SportsEventRegistration reg = SportsEventRegistration.builder()
                .event(event)
                .user(user)
                .category(category)
                .matchType(SportsEvent.MatchFormat.valueOf(req.getMatchType()))
                .status(initialStatus)
                .playerName(pName)
                .email(email)
                .relation(req.getRelation())
                .flatNumber(req.getFlatNumber())
                .age(req.getAge() != null ? req.getAge() : age)
                .role(req.getRole())
                .registeredAt(LocalDateTime.now())
                .build();

        if (req.getPartnerUserId() != null)
            reg.setPartner(userRepo.findById(req.getPartnerUserId()).orElseThrow(() -> new ResourceNotFoundException("AppUser", req.getPartnerUserId())));

        SportsEventRegistration saved = regRepo.save(reg);

        if (!approvalRequired) {
            handleConfirmationSideEffects(saved);
        }

        // Email the right stage: "we received your entry" (pending) vs. "you're confirmed".
        registrationEmailService.send(saved, approvalRequired
                ? RegistrationEmailService.Stage.RECEIVED
                : RegistrationEmailService.Stage.CONFIRMED);

        return saved;
    }

    /** Null-safe, case-insensitive, trimmed equality (treats null and blank as equal). */
    private static boolean normEq(String a, String b) {
        return java.util.Objects.equals(
                a == null ? "" : a.trim().toLowerCase(),
                b == null ? "" : b.trim().toLowerCase());
    }

    @Transactional
    public void withdraw(Long registrationId, Long userId) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));

        if (!reg.getUser().getId().equals(userId))
            throw new UnauthorizedActionException("You can only withdraw your own registration.");

        reg.setStatus(SportsEventRegistration.RegistrationStatus.WITHDRAWN);
        regRepo.save(reg);

        try {
            String eventName = reg.getEvent() != null ? reg.getEvent().getName() : "the event";
            notificationService.createNotification(
                    userId, NotificationType.REGISTRATION_WITHDRAWN, NotificationCategory.EVENTS,
                    "Registration Withdrawn",
                    "Your registration for " + eventName + " has been withdrawn",
                    null, ReferenceType.SPORTS_EVENT,
                    reg.getEvent() != null ? reg.getEvent().getId() : null,
                    NotificationPriority.NORMAL, null, null);
        } catch (Exception e) {
            log.warn("Failed to persist withdrawal notification: {}", e.getMessage());
        }
    }

    @Override
    public List<SportsEventRegistration> getEventRegistrations(Long eventId) {
        List<SportsEventRegistration> regs = regRepo.findByEventId(eventId);
        hydrateCaptaincy(regs, eventId);
        return regs;
    }

    @Override
    public List<SportsEventRegistration> getUserRegistrations(Long userId) {
        List<SportsEventRegistration> regs = regRepo.findByUserId(userId);
        // Hydrate each registration with its event's captaincy info
        for (SportsEventRegistration reg : regs) {
            hydrateCaptaincy(List.of(reg), reg.getEvent().getId());
        }
        return regs;
    }

    @Override
    @Transactional
    public SportsEventRegistration confirmRegistration(Long registrationId) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));
        reg.setStatus(SportsEventRegistration.RegistrationStatus.CONFIRMED);
        SportsEventRegistration saved = regRepo.save(reg);

        handleConfirmationSideEffects(saved);

        // Registration process — the entry is now CONFIRMED.
        registrationEmailService.send(saved, RegistrationEmailService.Stage.CONFIRMED);

        return saved;
    }

    private void handleConfirmationSideEffects(SportsEventRegistration saved) {
        auctionConfigRepo.findByEventId(saved.getEvent().getId()).ifPresent(config -> {
            boolean exists = playerRepo.findByConfigId(config.getId()).stream()
                    .anyMatch(p -> p.getUser() != null && p.getUser().getId().equals(saved.getUser().getId()));
            if (!exists) {
                String cat = "Batsmen";
                if ("Bowler".equalsIgnoreCase(saved.getRole())) cat = "Bowler";
                else if ("All-rounder".equalsIgnoreCase(saved.getRole())) cat = "All-rounder";
                else if ("Wicket Keeper".equalsIgnoreCase(saved.getRole())) cat = "Wicket Keeper";

                long count = playerRepo.countByConfigId(config.getId());
                AuctionPlayer player = AuctionPlayer.builder()
                        .config(config)
                        .user(saved.getUser())
                        .playerName(saved.getPlayerName() != null && !saved.getPlayerName().isEmpty() ? saved.getPlayerName() : saved.getUser().getFullName())
                        .category(cat)
                        .playerRole(saved.getRole() != null ? saved.getRole() : "Batsman")
                        .age(saved.getAge() != null ? saved.getAge() : 30)
                        .basePrice(config.getBasePrice() != null ? config.getBasePrice() : 1000)
                        .statsJson("{\"matches\":24,\"runs\":620,\"wickets\":18}")
                        .queueOrder((int) count + 1)
                        .status(AuctionPlayer.PlayerStatus.QUEUED)
                        .uploadedAt(LocalDateTime.now())
                        .build();
                playerRepo.save(player);
            }
        });

        hydrateCaptaincy(List.of(saved), saved.getEvent().getId());
    }

    @Override
    @Transactional
    public SportsEventRegistration rejectRegistration(Long registrationId, String reason) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEventRegistration", registrationId));
        reg.setStatus(SportsEventRegistration.RegistrationStatus.REJECTED);
        reg.setRejectReason(reason);
        SportsEventRegistration saved = regRepo.save(reg);

        // Registration process — the entry was not approved (optional reason).
        registrationEmailService.send(saved, RegistrationEmailService.Stage.REJECTED, reason);

        return saved;
    }

    @Override
    @Transactional
    public SportsEventRegistration nominateCaptain(Long registrationId, boolean nominate, String teamName) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
        
        AuctionConfig config = auctionConfigRepo.findByEventId(reg.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig for event", reg.getEvent().getId()));

        AuctionTeam team = auctionTeamRepo.findByConfigIdAndOwnerUserId(config.getId(), reg.getUser().getId())
                .orElseGet(() -> AuctionTeam.builder()
                        .config(config)
                        .ownerUser(reg.getUser())
                        .captainUser(reg.getUser())
                        .ownerName(reg.getUser().getFullName())
                        .eventId(reg.getEvent().getId())
                        .totalBudget(config.getBudgetPerTeam())
                        .remainingBudget(config.getBudgetPerTeam())
                        .spent(0L)
                        .build());

        team.setCaptainNomination(nominate);
        if (nominate) {
            team.setTeamName(teamName);
        }
        
        auctionTeamRepo.save(team);
        hydrateCaptaincy(List.of(reg), reg.getEvent().getId());
        return reg;
    }

    @Override
    @Transactional
    public SportsEventRegistration confirmCaptain(Long registrationId, boolean confirm) {
        SportsEventRegistration reg = regRepo.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", registrationId));
        
        AuctionConfig config = auctionConfigRepo.findByEventId(reg.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig for event", reg.getEvent().getId()));

        AuctionTeam team = auctionTeamRepo.findByConfigIdAndOwnerUserId(config.getId(), reg.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("AuctionTeam for user", reg.getUser().getId()));

        team.setCaptainConfirmation(confirm);
        auctionTeamRepo.save(team);
        
        hydrateCaptaincy(List.of(reg), reg.getEvent().getId());
        return reg;
    }

    private void hydrateCaptaincy(List<SportsEventRegistration> regs, Long eventId) {
        if (regs.isEmpty()) return;
        auctionConfigRepo.findByEventId(eventId).ifPresent(config -> {
            List<AuctionTeam> teams = auctionTeamRepo.findByConfigIdOrderByTeamName(config.getId());
            for (SportsEventRegistration reg : regs) {
                teams.stream()
                    .filter(t -> t.getOwnerUser() != null && t.getOwnerUser().getId().equals(reg.getUser().getId()))
                    .findFirst()
                    .ifPresent(t -> {
                        reg.setCaptainNomination(t.getCaptainNomination());
                        reg.setCaptainConfirmation(t.getCaptainConfirmation());
                        reg.setProposedTeamName(t.getTeamName());
                    });
            }
        });
    }

    @Transactional
    public SportsEvent updateStatus(Long id, String status) {
        Tournament.EventStatus tournamentStatus;
        try {
            tournamentStatus = Tournament.EventStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new ManaCommunityException(
                    "Invalid event status: '" + status + "'. Valid values: DRAFT, REGISTRATION_OPEN, "
                    + "REGISTRATION_CLOSED, LIVE, COMPLETED, CANCELLED.",
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }

        SportsEvent event = eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        Tournament tournament = event.getTournament();
        if (tournament != null) {
            tournament.setRegistrationStatus(tournamentStatus);
            tournamentRepo.save(tournament);
        }

        boolean isClosing = tournamentStatus == Tournament.EventStatus.COMPLETED
                || tournamentStatus == Tournament.EventStatus.CANCELLED;

        if (isClosing && tournament != null && tournament.getSportsEvents() != null) {
            for (SportsEvent sibling : tournament.getSportsEvents()) {
                sibling.setActive(false);
                sibling.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
                sibling.setUpdatedAt(LocalDateTime.now());
                eventRepo.save(sibling);
            }
        } else if (tournament != null && tournament.getSportsEvents() != null) {
            for (SportsEvent sibling : tournament.getSportsEvents()) {
                sibling.setActive(true);
                sibling.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
                sibling.setUpdatedAt(LocalDateTime.now());
                eventRepo.save(sibling);
            }
        }

        event.setUpdatedAt(LocalDateTime.now());
        if (isClosing) {
            event.setActive(false);
        } else {
            event.setActive(true);
        }
        event.setStatus(SportsEvent.EventStatus.valueOf(tournamentStatus.name()));
        SportsEvent saved = eventRepo.save(event);

        notifyEventParticipants(saved, tournamentStatus);

        return saved;
    }

    private LocalDateTime getTournamentStartDateTime(SportsEvent event) {
        if (event.getEventDateStart() == null) return null;
        java.time.LocalDate date = event.getEventDateStart();
        
        int hours = 9;
        int minutes = 0;
        
        String startTime = event.getStartTime();
        if (startTime != null && !startTime.trim().isEmpty()) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+):(\\d+)\\s*(AM|PM)?", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(startTime);
            if (matcher.find()) {
                hours = Integer.parseInt(matcher.group(1));
                minutes = Integer.parseInt(matcher.group(2));
                String ampm = matcher.group(3);
                if (ampm != null) {
                    if ("PM".equalsIgnoreCase(ampm) && hours < 12) {
                        hours += 12;
                    } else if ("AM".equalsIgnoreCase(ampm) && hours == 12) {
                        hours = 0;
                    }
                }
            }
        }
        
        return date.atTime(hours, minutes);
    }

    private void scheduleNotifications(SportsEvent event, List<NotificationScheduleDto> configs) {
        LocalDateTime eventStart = event.getEventDateStart().atTime(8, 0);
        LocalDateTime preciseStart = getTournamentStartDateTime(event);
        if (preciseStart == null) preciseStart = eventStart;

        for (NotificationScheduleDto cfg : configs) {
            if (cfg.getId() == null && cfg.getOffsetType() != null) {
                // Legacy DTO shape (seeders / test mocks) — map onto the rich scheduler.
                LocalDateTime notifyAt = switch (cfg.getOffsetType()) {
                    case "DAYS"    -> eventStart.minusDays(cfg.getOffsetValue());
                    case "HOURS"   -> eventStart.minusHours(cfg.getOffsetValue());
                    case "MINUTES" -> eventStart.minusMinutes(cfg.getOffsetValue());
                    default        -> eventStart;
                };
                schedulerRepo.save(SportsNotificationScheduler.builder()
                        .event(event)
                        .triggerKey(cfg.getType())
                        .label(cfg.getType() != null ? cfg.getType() : "Reminder")
                        .offsetMinutes(0)
                        .enabled(true)
                        .title(cfg.getTitle() != null ? cfg.getTitle() : "Reminder")
                        .body(cfg.getBody() != null ? cfg.getBody() : "")
                        .recipients("Registered Players")
                        .channels("push,email")
                        .priority("NORMAL")
                        .isCustom(true)
                        .sent(false)
                        .notifyAt(notifyAt)
                        .build());
            } else {
                // Premium interactive multi-channel scheduler support
                int offsetMinutes = cfg.getOffset();
                LocalDateTime notifyAt = preciseStart.plusMinutes(offsetMinutes);
                
                String recipients = cfg.getRecipients() != null 
                        ? String.join(",", cfg.getRecipients()) 
                        : "Registered Players";
                String channels = cfg.getOverrideChannels() != null 
                        ? String.join(",", cfg.getOverrideChannels()) 
                        : "push,email";
                
                schedulerRepo.save(SportsNotificationScheduler.builder()
                        .event(event)
                        .triggerKey(cfg.getId())
                        .label(cfg.getLabel() != null ? cfg.getLabel() : "Custom Trigger")
                        .offsetMinutes(offsetMinutes)
                        .enabled(cfg.isEnabled())
                        .title(cfg.getTitle())
                        .body(cfg.getBody())
                        .recipients(recipients)
                        .channels(channels)
                        .priority(cfg.getPriority() != null ? cfg.getPriority().toUpperCase() : "NORMAL")
                        .isCustom(cfg.isCustom())
                        .sent(false)
                        .notifyAt(notifyAt)
                        .build());
            }
        }
    }

    public List<SportsEvent> getMyEvents(Long userId) {
        return eventRepo.findEventsForUser(userId);
    }

    public List<SportsEvent> getOpenEvents(Long communityId) {
        return eventRepo.findByCommunityIdAndTournamentRegistrationStatusInOrderByEventDateStartAsc(
                communityId,
                List.of(Tournament.EventStatus.REGISTRATION_OPEN));
    }

    @Override
    public List<SportsEvent> getAllOpenEvents() {
        return eventRepo.findByTournamentRegistrationStatusOrderByEventDateStartAsc(Tournament.EventStatus.REGISTRATION_OPEN);
    }

    @Override
    public List<SportsEvent> getClosedEvents() {
        List<SportsEvent> events = eventRepo.findByTournamentRegistrationStatusOrderByEventDateStartAsc(Tournament.EventStatus.REGISTRATION_CLOSED);
        for (SportsEvent event : events) {
            auctionConfigRepo.findByEventId(event.getId()).ifPresent(config -> {
                event.setAuctionStatus(SportsEvent.AuctionEventStatus.valueOf(config.getStatus().name()));
            });
        }
        return events;
    }

    @Override
    public List<SportsEvent> getClosedEvents(Long communityId) {
        List<SportsEvent> events = eventRepo.findByCommunityIdAndTournamentRegistrationStatusInOrderByEventDateStartAsc(
                communityId, List.of(Tournament.EventStatus.REGISTRATION_CLOSED));
        for (SportsEvent event : events) {
            auctionConfigRepo.findByEventId(event.getId()).ifPresent(config -> {
                event.setAuctionStatus(SportsEvent.AuctionEventStatus.valueOf(config.getStatus().name()));
            });
        }
        return events;
    }

    @Transactional
    public void syncTournaments() {
        List<SportsEvent> events = eventRepo.findAll();
        for (SportsEvent event : events) {
            boolean exists = tournamentRepo.existsByEventId(event.getId());
            if (!exists) {
                Tournament tournament = Tournament.builder()
                        .name(event.getName())
                        .sportsEvents(new java.util.ArrayList<>())
                        .community(event.getCommunity())
                        .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : LocalDateTime.now())
                        .updatedAt(event.getUpdatedAt() != null ? event.getUpdatedAt() : LocalDateTime.now())
                        .build();
                event.setTournament(tournament);
                tournament.getSportsEvents().add(event);
                tournamentRepo.save(tournament);
            }
        }
    }

    @Override
    public List<SportsEvent> getAllEvents() {
        return eventRepo.findByActiveTrue();
    }

    @Override
    public Page<SportsEvent> getAllEvents(Pageable pageable) {
        return eventRepo.findByActiveTrue(pageable);
    }

    @Override
    public List<SportsEvent> getAllEventsIncludingInactive() {
        return eventRepo.findAll();
    }

    @Override
    public List<SportsEvent> getCommunityEvents(Long communityId) {
        return eventRepo.findByCommunityIdAndActiveTrueOrderByEventDateStartDesc(communityId);
    }

    @Override
    public List<SportsEvent> getCommunityEventsIncludingInactive(Long communityId) {
        return eventRepo.findByCommunityIdOrderByEventDateStartDesc(communityId);
    }

    @Override
    public Page<SportsEvent> getCommunityEvents(Long communityId, Pageable pageable) {
        return eventRepo.findByCommunityIdAndActiveTrue(communityId, pageable);
    }

    @Override
    public SportsEvent getEventById(Long id) {
        return eventRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    @Override
    public SportsEvent getEventByUuid(java.util.UUID uuid) {
        return eventRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "uuid", String.valueOf(uuid)));
    }

    @Override
    public SportsEvent saveEvent(SportsEvent event) {
        return eventRepo.save(event);
    }

    @Override
    public List<java.util.Map<String, Object>> getEventMap(Long communityId) {
        List<SportsEvent> events;
        if (communityId == null) {
            events = eventRepo.findAll();
        } else {
            events = eventRepo.findByCommunityIdOrderByEventDateStartDesc(communityId);
        }
        return events.stream().map(e -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", e.getId());
            map.put("name", e.getName());
            return map;
        }).toList();
    }

    @Override
    public long getConfirmedRegistrationCount(Long eventId) {
        return regRepo.countByEventIdAndStatus(eventId, SportsEventRegistration.RegistrationStatus.CONFIRMED);
    }

    @Override
    @Transactional
    public void deleteEvent(Long eventId) {
        tournamentRepo.deleteByEventId(eventId);
        eventRepo.deleteById(eventId);
    }

    @Override
    @Transactional
    public SportsEvent updateEvent(Long eventId, SportsEventRequest req) {
        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        
        event.setName(req.getName());
        event.setEventDateStart(req.getEventDateStart());
        event.setEventDateEnd(req.getEventDateEnd());
        event.setRegistrationDateStart(req.getRegistrationDateStart());
        event.setRegistrationDateEnd(req.getRegistrationDateEnd());
        if (req.getVenueId() != null) {
            event.setVenue(venueRepo.findById(req.getVenueId()).orElseThrow(() -> new ResourceNotFoundException("Venue", req.getVenueId())));
        }
        event.setMaxParticipants(req.getMaxParticipants());

        if (req.getCategoryIds() != null) {
            event.setCategories(new java.util.HashSet<>(categoryRepo.findAllById(req.getCategoryIds())));
        }
        
        event.setDisputeCommittee(resolveDisputeCommittee(req.getDisputeCommitteeIds()));
        event.setMinPlayers(req.getMinPlayers());
        event.setMaxPlayers(req.getMaxPlayers());
        event.setGender(req.getGender());
        event.setPlayersBorn(req.getPlayersBorn());
        event.setFormat(req.getFormat() != null ? java.util.Arrays.asList(req.getFormat().split(",")) : new java.util.ArrayList<>());
        event.setTournamentType(req.getTournamentType() != null
                ? SportsEvent.TournamentType.valueOf(req.getTournamentType()) : null);
        
        event.setContactName(req.getContactName());
        event.setContactNumber(req.getContactNumber());
        event.setContactEmail(req.getContactEmail());
        event.getContacts().clear();
        event.getContacts().addAll(resolveContacts(req.getContacts()));
        event.setOtherContacts(req.getOtherContacts());
        if (req.getAuctionEnabled() != null) {
            event.setAuctionEnabled(req.getAuctionEnabled());
        }
        event.setBannerImage(req.getBannerImage());
        event.setTournamentLevel(req.getTournamentLevel());
        event.setDescription(req.getDescription());
        event.setStartTime(req.getStartTime());
        event.setDueTime(req.getDueTime());
        if (req.getMinAge() != null) {
            event.setMinAge(req.getMinAge());
        }
        if (req.getMaxAge() != null) {
            event.setMaxAge(req.getMaxAge());
        }
        if (req.getAdminApprovalRequired() != null) {
            event.setAdminApprovalRequired(req.getAdminApprovalRequired());
        }

        event.setUpdatedAt(LocalDateTime.now());
        
        if (event.getSponsors() != null) {
            event.getSponsors().clear();
        } else {
            event.setSponsors(new java.util.ArrayList<>());
        }
        if (req.getSponsors() != null) {
            for (SponsorDto s : req.getSponsors()) {
                event.getSponsors().add(SportsEventSponsor.builder()
                        .event(event)
                        .category(s.getCategory())
                        .name(s.getName())
                        .url(s.getUrl())
                        .build());
            }
        }
        
        SportsEvent saved = eventRepo.save(event);
        
        // Clean and reschedule notification rules on tournament update
        if (req.getNotifications() != null) {
            schedulerRepo.deleteByEventId(saved.getId());
            scheduleNotifications(saved, req.getNotifications());
        }

        return saved;
    }



    private java.util.Set<AppUser> resolveDisputeCommittee(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new java.util.HashSet<>();
        return new java.util.HashSet<>(userRepo.findAllById(userIds));
    }

    @Override
    @Transactional
    public SportsEvent updateDisputeCommittee(Long eventId, List<Long> userIds) {
        SportsEvent event = eventRepo.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SportsEvent", eventId));
        event.setDisputeCommittee(resolveDisputeCommittee(userIds));
        return eventRepo.save(event);
    }

     private void notifyEventParticipants(SportsEvent event, Tournament.EventStatus newStatus) {
        try {
            List<Long> userIds = regRepo.findByEventId(event.getId()).stream()
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
                    title = "Registrations Open — " + event.getName();
                    body = "Registrations are now open. Sign up before spots fill!";
                    priority = NotificationPriority.HIGH;
                }
                case CANCELLED -> {
                    type = NotificationType.EVENT_CANCELLED;
                    title = "Event Cancelled — " + event.getName();
                    body = "This event has been cancelled";
                    priority = NotificationPriority.HIGH;
                }
                case COMPLETED -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = "Event Completed — " + event.getName();
                    body = "This event has concluded. Thank you for participating!";
                    priority = NotificationPriority.NORMAL;
                }
                default -> {
                    type = NotificationType.EVENT_STATUS_CHANGED;
                    title = event.getName() + " — Status Update";
                    body = "Event status changed to " + newStatus.name().replace('_', ' ').toLowerCase();
                    priority = NotificationPriority.NORMAL;
                }
            }

            notificationService.createBulkNotifications(
                    userIds, type, NotificationCategory.EVENTS,
                    title, body, null,
                    ReferenceType.SPORTS_EVENT, event.getId(),
                    priority, null,
                    event.getCommunity() != null ? event.getCommunity().getId() : null);
        } catch (Exception e) {
            log.warn("Failed to persist event-status notifications for event {}: {}", event.getId(), e.getMessage());
        }
    }

    private String listToCommaString(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }
}
