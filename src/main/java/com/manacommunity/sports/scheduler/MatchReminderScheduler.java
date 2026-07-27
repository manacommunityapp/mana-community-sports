package com.manacommunity.sports.scheduler;

import com.manacommunity.sports.email.EmailProperties;
import com.manacommunity.sports.email.MatchReminderEmailService;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.model.AuctionPlayer;
import com.manacommunity.sports.model.AuctionTeam;
import com.manacommunity.sports.model.scheduler.TournamentMatch;
import com.manacommunity.sports.repository.scheduler.TournamentMatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emails each participant a "your match starts soon" reminder shortly before the
 * match's scheduled start. Runs every minute, picks up published matches falling
 * inside the configured lead window, mails the two teams' players, and flags the
 * match so it is never reminded twice.
 *
 * <p>Recipients for a match are the distinct {@link AppUser}s reachable from each
 * side: a team's roster (its {@link AuctionPlayer}s) plus its owner and captain.
 * For individual knockout draws each "team" is effectively one player, so this
 * naturally reduces to the two competitors.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchReminderScheduler {

    private final TournamentMatchRepository  matchRepo;
    private final MatchReminderEmailService  matchReminderEmailService;
    private final EmailProperties            props;

    @Scheduled(fixedDelay = 60_000) // every 60 seconds
    @Transactional
    public void sendDueMatchReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusMinutes(props.getMatchReminderLeadMinutes());

        List<TournamentMatch> due = matchRepo.findDueForReminder(now, cutoff);
        if (due.isEmpty()) return;

        for (TournamentMatch match : due) {
            try {
                List<AppUser> recipients = collectRecipients(match);
                long minutesUntilStart = Math.max(0,
                        Duration.between(now, match.getScheduledAt()).toMinutes());

                if (!recipients.isEmpty()) {
                    matchReminderEmailService.sendMatchReminder(match, recipients, minutesUntilStart);
                }
                // Flag regardless of recipient count so we don't re-scan this match every minute.
                match.setReminderSent(true);
                match.setReminderSentAt(now);
                matchRepo.save(match);
            } catch (Exception e) {
                log.error("Failed to send reminder for match {}", match.getId(), e);
            }
        }
    }

    /** Distinct, email-bearing users across both teams of the match. */
    private List<AppUser> collectRecipients(TournamentMatch match) {
        Map<Long, AppUser> byId = new LinkedHashMap<>();
        addTeamUsers(byId, match.getTeamA());
        addTeamUsers(byId, match.getTeamB());
        return new ArrayList<>(byId.values());
    }

    private void addTeamUsers(Map<Long, AppUser> byId, AuctionTeam team) {
        if (team == null) return;
        addUser(byId, team.getOwnerUser());
        addUser(byId, team.getCaptainUser());
        if (team.getPlayers() != null) {
            for (AuctionPlayer p : team.getPlayers()) {
                if (p != null) addUser(byId, p.getUser());
            }
        }
    }

    private void addUser(Map<Long, AppUser> byId, AppUser user) {
        if (user == null || user.getId() == null) return;
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        byId.putIfAbsent(user.getId(), user);
    }
}
