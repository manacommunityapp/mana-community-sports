package com.manacommunity.sports.ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Sends real-time push notifications to connected clients via the existing
 * STOMP/WebSocket infrastructure ({@code /ws} endpoint, {@code /topic/} broker).
 *
 * <p>Uses the same channel pattern as {@code ChatService}:
 * {@code /topic/notifications/{userId}} for user-specific pushes.
 * The mobile/web client subscribes to this topic on connect and renders
 * incoming events as popup alerts, banners, or badge updates.</p>
 *
 * <p>This complements (does not replace) in-app notifications stored in the
 * database via {@code NotificationManagementService}. WebSocket push is
 * instant but ephemeral — if the user isn't connected, they see it next
 * time they open the notification inbox.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiWebSocketPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /** Payload sent over the WebSocket to the client. */
    public record PushEvent(
            String type,        // MATCH_REMINDER, SCHEDULE_UPDATE, AI_ALERT, etc.
            String title,
            String body,
            String actionUrl,
            Long referenceId,
            LocalDateTime timestamp
    ) {
        public PushEvent(String type, String title, String body) {
            this(type, title, body, null, null, LocalDateTime.now());
        }

        public PushEvent(String type, String title, String body, String actionUrl, Long referenceId) {
            this(type, title, body, actionUrl, referenceId, LocalDateTime.now());
        }
    }

    /**
     * Push a notification to a single user's connected clients.
     */
    public void pushToUser(Long userId, PushEvent event) {
        String destination = "/topic/notifications/" + userId;
        messagingTemplate.convertAndSend(destination, event);
        log.debug("WebSocket push to user {}: type={}, title='{}'", userId, event.type(), event.title());
    }

    /**
     * Push a notification to multiple users.
     */
    public void pushToUsers(List<Long> userIds, PushEvent event) {
        for (Long userId : userIds) {
            pushToUser(userId, event);
        }
        log.info("WebSocket push to {} users: type={}", userIds.size(), event.type());
    }

    /**
     * Push a match reminder to a specific user.
     */
    public void pushMatchReminder(Long userId, String teamA, String teamB,
                                   String scheduledAt, String venue, Long matchId) {
        PushEvent event = new PushEvent(
                "MATCH_REMINDER",
                "⚡ " + teamA + " vs " + teamB,
                scheduledAt + " at " + venue,
                "/profile?tab=schedule",
                matchId);
        pushToUser(userId, event);
    }

    /**
     * Push a schedule update notification to a user.
     */
    public void pushScheduleUpdate(Long userId, String tournamentName, int matchCount) {
        PushEvent event = new PushEvent(
                "SCHEDULE_UPDATE",
                "📅 Schedule Updated — " + tournamentName,
                matchCount + " matches in your schedule",
                "/profile?tab=schedule",
                null);
        pushToUser(userId, event);
    }

    /**
     * Push an AI assistant notification to a user (e.g. digest ready, action completed).
     */
    public void pushAiAlert(Long userId, String title, String body) {
        PushEvent event = new PushEvent("AI_ALERT", title, body);
        pushToUser(userId, event);
    }
}
