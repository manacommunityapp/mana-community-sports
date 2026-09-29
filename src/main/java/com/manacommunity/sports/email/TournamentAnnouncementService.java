package com.manacommunity.sports.email;

import com.manacommunity.common.util.EmailTemplateRenderer;
import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.dto.TournamentAnnouncementRequest;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.service.NotificationManagementService;
import com.manacommunity.common.user.model.AppUser;
import com.manacommunity.common.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentAnnouncementService {

    private final EmailSupport support;
    private final EmailTemplateRenderer renderer;
    private final EmailService emailService;
    private final NotificationManagementService notificationService;
    private final AppUserRepository userRepo;
    private final TournamentEmailService tournamentEmailService;

    public int announce(Tournament tournament, TournamentAnnouncementRequest req) {
        Long communityId = tournament.getCommunity() != null ? tournament.getCommunity().getId() : null;
        if (communityId == null) {
            log.warn("Tournament {} has no community — cannot send announcement", tournament.getId());
            return 0;
        }

        List<AppUser> recipients = userRepo.findByCommunityIdAndIsActiveTrue(communityId);
        if (recipients.isEmpty()) return 0;

        EmailTemplate template = resolveTemplate(req.template());
        String subject = req.subject();

        int sent = 0;
        for (AppUser user : recipients) {
            try {
                String html;
                if (req.customHtml() != null && !req.customHtml().isBlank()) {
                    html = req.customHtml();
                } else {
                    // Single DTO drives the template — built once per recipient with
                    // the per-send custom message applied.
                    Map<String, Object> vars = support.baseVars(user.getFullName());
                    vars.put("email", tournamentEmailService.buildTournamentAnnouncementDTO(tournament, req.message()));
                    html = renderer.render(template, vars);
                }

                if (req.sendEmail()) {
                    emailService.send(new EmailMessage(
                            user.getEmail(), user.getFullName(), subject, html));
                }

                if (req.sendPush()) {
                    notificationService.createEmailNotification(
                            user.getId(),
                            template == EmailTemplate.TOURNAMENT_OPEN
                                    ? NotificationType.TOURNAMENT_OPEN
                                    : NotificationType.TOURNAMENT_ANNOUNCEMENT,
                            NotificationCategory.SPORTS,
                            subject,
                            req.message(),
                            support.props().getBaseUrl() + "/sports",
                            ReferenceType.TOURNAMENT,
                            tournament.getId(),
                            NotificationPriority.HIGH);
                }
                sent++;
            } catch (Exception e) {
                log.error("Failed to send announcement to user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Tournament announcement sent to {}/{} recipients for tournament {}",
                sent, recipients.size(), tournament.getId());
        return sent;
    }

    private EmailTemplate resolveTemplate(String templateName) {
        if (templateName == null || templateName.isBlank()) return EmailTemplate.TOURNAMENT_ANNOUNCEMENT;
        try {
            return EmailTemplate.valueOf(templateName);
        } catch (IllegalArgumentException e) {
            return EmailTemplate.TOURNAMENT_ANNOUNCEMENT;
        }
    }

}




