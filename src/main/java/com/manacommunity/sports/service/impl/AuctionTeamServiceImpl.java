package com.manacommunity.sports.service.impl;

import com.manacommunity.sports.security.AuditAction;

import com.manacommunity.sports.security.AuditModule;

import com.manacommunity.sports.security.AuditService;

import com.manacommunity.sports.dto.AuctionTeamRequest;
import com.manacommunity.sports.exception.ResourceNotFoundException;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.user.repository.AppUserRepository;
import com.manacommunity.sports.repository.AuctionConfigRepository;
import com.manacommunity.sports.repository.AuctionTeamRepository;
import com.manacommunity.sports.service.AuctionTeamService;
import com.manacommunity.sports.service.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionTeamServiceImpl implements AuctionTeamService {

    private final AuctionTeamRepository teamRepo;
    private final AuctionConfigRepository configRepo;
    private final AppUserRepository userRepo;
    private final com.manacommunity.sports.security.AuditService auditService;
    private final NotificationManagementService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getTeams(Long configId) {
        return teamRepo.findByConfigIdOrderByTeamName(configId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getNominatedCaptains(Long eventId) {
        AuctionConfig config = configRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig for event", eventId));
        return teamRepo.findByConfigIdAndCaptainNominationTrue(config.getId());
    }

    @Override
    @Transactional
    public AuctionTeam createTeam(AuctionTeamRequest req, Long adminUserId) {
        AuctionConfig config = configRepo.findById(req.configId())
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig", req.configId()));
        
        Long ownerUserId = req.ownerUserId() != null ? req.ownerUserId() : adminUserId;
        AppUser teamOwner = userRepo.findById(ownerUserId).orElseThrow(() -> new ResourceNotFoundException("AppUser", ownerUserId));
        AuctionTeam team = AuctionTeam.builder()
            .config(config)
            .teamName(req.teamName())
            .ownerName(req.ownerName())
            .ownerUser(teamOwner)
            .captainUser(teamOwner)
            .eventId(config.getEvent() != null ? config.getEvent().getId() : null)
            .colorHex(req.colorHex())
            .totalBudget(req.totalBudget())
            .remainingBudget(req.totalBudget())
            .spent(0L)
            .captainNomination(false)
            .captainConfirmation(false)
            .build();

        AuctionTeam savedTeam = teamRepo.save(team);
        auditService.record(
            com.manacommunity.sports.security.AuditAction.TEAM_CREATED,
            com.manacommunity.sports.security.AuditModule.AUCTION,
            "AuctionTeam", String.valueOf(savedTeam.getId()),
            null,
            "name=" + savedTeam.getTeamName() + ", budget=" + savedTeam.getTotalBudget());

        try {
            notificationService.createNotification(
                    teamOwner.getId(), NotificationType.TEAM_CREATED, NotificationCategory.AUCTION,
                    "Team Created — " + savedTeam.getTeamName(),
                    "Budget: ₹" + savedTeam.getTotalBudget(),
                    null, ReferenceType.AUCTION_TEAM, savedTeam.getId(),
                    NotificationPriority.NORMAL, null, null);
        } catch (Exception e) {
            log.warn("Failed to persist team-created notification: {}", e.getMessage());
        }

        return savedTeam;
    }

    @Override
    @Transactional
    public AuctionTeam confirmCaptain(Long teamId, boolean confirm, Long callerUserId, boolean isAdmin) {
        AuctionTeam team = teamRepo.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionTeam", teamId));

        if (!isAdmin) {
            boolean isOwner = team.getOwnerUser() != null && team.getOwnerUser().getId().equals(callerUserId);
            boolean isCaptain = team.getCaptainUser() != null && team.getCaptainUser().getId().equals(callerUserId);
            if (!isOwner && !isCaptain) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "You can only confirm captaincy for your own team");
            }
        }

        team.setCaptainConfirmation(confirm);
        AuctionTeam saved = teamRepo.save(team);

        try {
            Long recipientId = saved.getCaptainUser() != null ? saved.getCaptainUser().getId()
                    : (saved.getOwnerUser() != null ? saved.getOwnerUser().getId() : null);
            if (recipientId != null && confirm) {
                notificationService.createNotification(
                        recipientId, NotificationType.CAPTAIN_CONFIRMED, NotificationCategory.AUCTION,
                        "Captain Confirmed — " + saved.getTeamName(),
                        "You are confirmed as captain",
                        null, ReferenceType.AUCTION_TEAM, saved.getId(),
                        NotificationPriority.NORMAL, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist captain-confirmed notification: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public AuctionTeam nominateCaptain(Long eventId, Long userId, boolean nominate, String teamName) {
        AuctionConfig config = configRepo.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("AuctionConfig for event", eventId));
        
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        AuctionTeam team = teamRepo.findByConfigIdAndOwnerUserId(config.getId(), userId)
                .orElseGet(() -> AuctionTeam.builder()
                        .config(config)
                        .ownerUser(user)
                        .captainUser(user)
                        .ownerName(user.getFullName())
                        .eventId(eventId)
                        .totalBudget(config.getBudgetPerTeam())
                        .remainingBudget(config.getBudgetPerTeam())
                        .spent(0L)
                        .build());

        team.setCaptainNomination(nominate);
        if (teamName != null && !teamName.isEmpty()) {
            team.setTeamName(teamName);
        } else if (team.getTeamName() == null) {
            team.setTeamName(user.getFullName() + "'s Team");
        }

        if (!nominate) {
            team.setCaptainConfirmation(false);
        }

        AuctionTeam saved = teamRepo.save(team);

        try {
            if (nominate) {
                notificationService.createNotification(
                        user.getId(), NotificationType.CAPTAIN_NOMINATED, NotificationCategory.AUCTION,
                        "Captain Nomination Submitted",
                        "You've been nominated as captain for " + saved.getTeamName(),
                        null, ReferenceType.AUCTION_TEAM, saved.getId(),
                        NotificationPriority.NORMAL, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to persist captain-nomination notification: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getMyNominations(Long userId) {
        return teamRepo.findByOwnerUserIdOrCaptainUserId(userId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionTeam> getCaptainRegistration(Long userId) {
        return teamRepo.findByOwnerUserIdOrCaptainUserId(userId, userId);
    }
}
