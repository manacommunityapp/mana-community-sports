package com.manacommunity.sports.service.scheduler;

import com.manacommunity.sports.email.ScheduleEmailService;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.SportsEventRegistration;
import com.manacommunity.sports.model.scheduler.TournamentConfig;
import com.manacommunity.sports.repository.SportsEventRegistrationRepository;
import com.manacommunity.sports.repository.scheduler.TournamentConfigRepository;
import com.manacommunity.sports.repository.scheduler.TournamentMatchRepository;
import com.manacommunity.sports.scheduler.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Sends participant notifications for scheduler events. Maps to the "NotificationService"
 * node of the scheduler service tree. Resolves the tournament's confirmed registrants
 * (same pattern as {@code NotificationScheduler}) and pushes via {@link PushNotificationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerNotificationService {

    private final TournamentConfigRepository        configRepo;
    private final SportsEventRegistrationRepository registrationRepo;
    private final TournamentMatchRepository         matchRepo;
    private final PushNotificationService           pushService;
    private final ScheduleEmailService              scheduleEmailService;

    /**
     * Notify a tournament's confirmed participants that its schedule was published.
     * Loads the config in its own read-only transaction so it is safe to call
     * AFTER the persistence transaction has committed (a rollback never notifies).
     * No-op when the config has no linked event or no confirmed registrants.
     */
    @Transactional(readOnly = true)
    public void notifySchedulePublished(Long configId) {
        TournamentConfig config = configRepo.findById(configId).orElse(null);
        if (config == null || config.getEvent() == null) {
            log.debug("Skipping publish notification — config {} missing or has no linked event", configId);
            return;
        }

        Long eventId = config.getEvent().getId();
        List<AppUser> recipients = registrationRepo
            .findByEventIdAndStatus(eventId, SportsEventRegistration.RegistrationStatus.CONFIRMED)
            .stream()
            .map(SportsEventRegistration::getUser)
            .filter(Objects::nonNull)
            .toList();

        if (recipients.isEmpty()) {
            log.info("No confirmed participants to notify for tournament '{}' (event {})",
                config.getTournamentName(), eventId);
            return;
        }

        String title = "Schedule published: " + config.getTournamentName();
        String body  = "The match schedule for " + config.getTournamentName()
            + " is now live. Check your fixtures.";
        pushService.sendBulk(recipients, title, body);

        // Email the same audience an HTML "schedule is live" notification.
        int matchCount = (int) matchRepo.countByConfigId(configId);
        scheduleEmailService.sendSchedulePublished(config, recipients, matchCount);

        log.info("Notified {} participants that schedule for '{}' was published",
            recipients.size(), config.getTournamentName());
    }
}
