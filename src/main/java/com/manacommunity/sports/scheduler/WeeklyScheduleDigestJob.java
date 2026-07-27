package com.manacommunity.sports.scheduler;

import com.manacommunity.sports.model.Community;

import com.manacommunity.sports.ai.service.AiWebSocketPushService;
import com.manacommunity.sports.email.EmailMessage;
import com.manacommunity.sports.email.EmailProperties;
import com.manacommunity.sports.email.EmailService;
import com.manacommunity.sports.email.EmailSupport;
import com.manacommunity.sports.user.model.AppUser;
import com.manacommunity.sports.user.repository.AppUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Sends a personalised "Your Week Ahead" email digest every Monday at 7:00 AM.
 *
 * <p>For each active user who has upcoming matches in the next 7 days, compiles
 * a formatted HTML email listing every match with team, venue, court, and time.
 * Also pushes a WebSocket notification so mobile clients can show a banner.</p>
 *
 * <p>Only runs when {@code app.mail.enabled=true} — dev/local environments
 * are safe by default.</p>
 *
 * <p>Uses the same match-discovery query as {@code MatchNotificationTools.getMyMatches}
 * but runs batch-style for all users in all communities at once.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyScheduleDigestJob {

    @PersistenceContext
    private EntityManager em;

    private final AppUserRepository userRepository;
    private final EmailService emailService;
    private final EmailProperties emailProps;
    private final EmailSupport emailSupport;
    private final AiWebSocketPushService webSocketPush;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("EEE dd MMM, hh:mm a");

    /**
     * Runs every Monday at 7:00 AM server time.
     * Cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "${app.digest.cron:0 0 7 * * MON}")
    @Transactional(readOnly = true)
    public void sendWeeklyDigest() {
        if (!emailProps.isEnabled()) {
            log.debug("[DIGEST] Email disabled — skipping weekly digest");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekEnd = now.plusDays(7);

        log.info("[DIGEST] Starting weekly schedule digest for {} to {}", now.toLocalDate(), weekEnd.toLocalDate());

        // Find all matches in the next 7 days with the users involved
        List<Object[]> matchRows = em.createQuery(
                "SELECT m.id, m.round, m.matchNumber, m.scheduledAt, m.durationMinutes, " +
                "ta.teamName, tb.teamName, ta.ownerUser.id, tb.ownerUser.id, " +
                "ta.captainUser.id, tb.captainUser.id, " +
                "v.name, c.name, tc.tournamentName, s.name, " +
                "ta.id, tb.id " +
                "FROM TournamentMatch m " +
                "LEFT JOIN m.teamA ta LEFT JOIN m.teamB tb " +
                "LEFT JOIN m.venue v LEFT JOIN m.court c " +
                "JOIN m.config tc LEFT JOIN tc.sport s " +
                "WHERE m.status IN ('SCHEDULED', 'PUBLISHED') " +
                "AND m.scheduledAt >= :now AND m.scheduledAt <= :weekEnd " +
                "ORDER BY m.scheduledAt ASC", Object[].class)
                .setParameter("now", now)
                .setParameter("weekEnd", weekEnd)
                .getResultList();

        if (matchRows.isEmpty()) {
            log.info("[DIGEST] No matches in the next 7 days — nothing to send");
            return;
        }

        // Also find players assigned to each team
        Set<Long> teamIds = new HashSet<>();
        for (Object[] mr : matchRows) {
            if (mr[15] != null) teamIds.add((Long) mr[15]); // teamA id
            if (mr[16] != null) teamIds.add((Long) mr[16]); // teamB id
        }

        // Map: teamId → list of player userIds
        Map<Long, List<Long>> teamPlayers = new HashMap<>();
        if (!teamIds.isEmpty()) {
            List<Object[]> playerRows = em.createQuery(
                    "SELECT ap.assignedTeam.id, ap.user.id FROM AuctionPlayer ap " +
                    "WHERE ap.assignedTeam.id IN :tids AND ap.user IS NOT NULL",
                    Object[].class)
                    .setParameter("tids", teamIds)
                    .getResultList();
            for (Object[] pr : playerRows) {
                teamPlayers.computeIfAbsent((Long) pr[0], k -> new ArrayList<>()).add((Long) pr[1]);
            }
        }

        // Build per-user match lists
        Map<Long, List<MatchDigestEntry>> userMatches = new LinkedHashMap<>();

        for (Object[] mr : matchRows) {
            MatchDigestEntry entry = new MatchDigestEntry(
                    mr[1] != null ? mr[1].toString() : "Match",
                    (Integer) mr[2],
                    (LocalDateTime) mr[3],
                    mr[5] != null ? (String) mr[5] : "TBD",
                    mr[6] != null ? (String) mr[6] : "TBD",
                    mr[11] != null ? (String) mr[11] : "TBA",
                    mr[12] != null ? (String) mr[12] : "",
                    mr[13] != null ? (String) mr[13] : "Tournament",
                    mr[14] != null ? (String) mr[14] : "");

            // Collect all user IDs involved in this match
            Set<Long> involvedUsers = new HashSet<>();
            if (mr[7] != null) involvedUsers.add((Long) mr[7]);   // teamA owner
            if (mr[8] != null) involvedUsers.add((Long) mr[8]);   // teamB owner
            if (mr[9] != null) involvedUsers.add((Long) mr[9]);   // teamA captain
            if (mr[10] != null) involvedUsers.add((Long) mr[10]); // teamB captain

            // Players on each team
            Long teamAId = (Long) mr[15];
            Long teamBId = (Long) mr[16];
            if (teamAId != null && teamPlayers.containsKey(teamAId)) {
                involvedUsers.addAll(teamPlayers.get(teamAId));
            }
            if (teamBId != null && teamPlayers.containsKey(teamBId)) {
                involvedUsers.addAll(teamPlayers.get(teamBId));
            }

            for (Long uid : involvedUsers) {
                userMatches.computeIfAbsent(uid, k -> new ArrayList<>()).add(entry);
            }
        }

        // Send emails
        int emailsSent = 0;
        for (Map.Entry<Long, List<MatchDigestEntry>> entry : userMatches.entrySet()) {
            Long userId = entry.getKey();
            List<MatchDigestEntry> matches = entry.getValue();

            AppUser user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) continue;
            if (!Boolean.TRUE.equals(user.getIsActive())) continue;

            String html = buildDigestHtml(user.getFullName(), matches);
            emailService.send(new EmailMessage(
                    user.getEmail(), user.getFullName(),
                    "📅 Your Week Ahead — " + matches.size() + " match"
                            + (matches.size() > 1 ? "es" : ""),
                    html));

            // Also push via WebSocket
            try {
                webSocketPush.pushAiAlert(userId,
                        "📅 Weekly Schedule",
                        matches.size() + " match" + (matches.size() > 1 ? "es" : "")
                                + " this week — check your email for details");
            } catch (Exception e) {
                log.debug("WebSocket push skipped for user {} (not connected)", userId);
            }

            emailsSent++;
        }

        log.info("[DIGEST] Sent {} weekly digest emails covering {} matches",
                emailsSent, matchRows.size());
    }

    // ── HTML builder ───────────────────────────────────────────────────

    private String buildDigestHtml(String userName, List<MatchDigestEntry> matches) {
        StringBuilder html = new StringBuilder();
        html.append("<div style='font-family:Arial,sans-serif;max-width:600px;margin:0 auto;'>");
        html.append("<h2 style='color:#2563eb;'>📅 Your Week Ahead</h2>");
        html.append("<p>Hi ").append(userName != null ? userName : "there").append(",</p>");
        html.append("<p>You have <strong>").append(matches.size()).append(" match");
        if (matches.size() > 1) html.append("es");
        html.append("</strong> coming up this week:</p>");

        for (int i = 0; i < matches.size(); i++) {
            MatchDigestEntry m = matches.get(i);
            html.append("<div style='background:#f8fafc;border-left:4px solid #2563eb;");
            html.append("padding:12px 16px;margin:12px 0;border-radius:4px;'>");
            html.append("<div style='font-size:13px;color:#64748b;'>")
                    .append(m.tournament).append(" — ").append(m.round).append("</div>");
            html.append("<div style='font-size:18px;font-weight:bold;margin:4px 0;'>")
                    .append(m.teamA).append(" vs ").append(m.teamB).append("</div>");
            html.append("<div>🗓 ").append(m.scheduledAt != null
                    ? m.scheduledAt.format(DISPLAY_FMT) : "TBA").append("</div>");
            html.append("<div>📍 ").append(m.venue);
            if (!m.court.isEmpty()) html.append(" — ").append(m.court);
            html.append("</div>");
            html.append("</div>");
        }

        html.append("<p style='margin-top:20px;'>")
                .append("<a href='").append(emailProps.getBaseUrl()).append("/profile?tab=schedule' ")
                .append("style='background:#2563eb;color:white;padding:10px 24px;")
                .append("text-decoration:none;border-radius:6px;display:inline-block;'>")
                .append("View Full Schedule</a></p>");
        html.append("<p style='color:#94a3b8;font-size:12px;margin-top:24px;'>")
                .append("— Mana Community</p>");
        html.append("</div>");

        return html.toString();
    }

    private record MatchDigestEntry(
            String round, Integer matchNumber, LocalDateTime scheduledAt,
            String teamA, String teamB, String venue, String court,
            String tournament, String sport) {}
}
