package com.manacommunity.sports.email;

import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.*;
import com.manacommunity.sports.model.scheduler.TournamentConfig;
import com.manacommunity.sports.service.NotificationManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Emails confirmed participants that a tournament's match schedule has been published.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleEmailService {

    private final EmailSupport                   support;
    private final EmailTemplateRenderer          renderer;
    private final EmailService                   emailService;
    private final NotificationManagementService  notificationService;

    public void sendSchedulePublished(TournamentConfig config, List<AppUser> recipients, int matchCount) {
        if (config == null || recipients == null || recipients.isEmpty()) return;

        SportsEvent event = config.getEvent();
        String venueName = config.getVenue() != null ? config.getVenue().getName()
                : (event != null && event.getVenue() != null ? event.getVenue().getName() : "TBA");

        List<EmailMessage> batch = new ArrayList<>();
        for (AppUser user : recipients) {
            if (user == null || support.isBlank(user.getEmail())) continue;
            try {
                Map<String, Object> vars = support.baseVars(user.getFullName());
                vars.put("tournamentName", config.getTournamentName());
                vars.put("eventName", event != null ? event.getName() : config.getTournamentName());
                vars.put("sportName", config.getSport() != null ? config.getSport().getName() : "");
                vars.put("startDate", support.formatDate(config.getStartDate()));
                vars.put("venueName", venueName);
                vars.put("matchCount", matchCount);
                vars.put("actionUrl", support.props().getBaseUrl() + "/profile?tab=schedule");

                String subject = EmailTemplate.SCHEDULE_PUBLISHED.defaultSubject()
                        + " — " + config.getTournamentName();
                String html = renderer.render(EmailTemplate.SCHEDULE_PUBLISHED, vars);
                batch.add(new EmailMessage(user.getEmail(), user.getFullName(), subject, html));
            } catch (Exception e) {
                log.error("Failed to build 'schedule published' email for user {}", user.getId(), e);
            }
        }
        emailService.sendAll(batch);
        log.info("Queued {} 'schedule published' emails for tournament '{}'",
                batch.size(), config.getTournamentName());

        try {
            List<Long> userIds = recipients.stream()
                    .filter(u -> u != null && u.getId() != null && !support.isBlank(u.getEmail()))
                    .map(AppUser::getId)
                    .toList();
            if (!userIds.isEmpty()) {
                String subject = EmailTemplate.SCHEDULE_PUBLISHED.defaultSubject()
                        + " — " + config.getTournamentName();
                notificationService.createBulkEmailNotifications(
                        userIds, NotificationType.SCHEDULE_PUBLISHED, NotificationCategory.SPORTS,
                        subject, matchCount + " matches scheduled for " + config.getTournamentName(),
                        support.props().getBaseUrl() + "/profile?tab=schedule",
                        ReferenceType.TOURNAMENT_CONFIG, config.getId(),
                        NotificationPriority.NORMAL);
            }
        } catch (Exception e) {
            log.warn("Failed to persist schedule-published notifications: {}", e.getMessage());
        }
    }
}
