package com.manacommunity.sports.service;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.sports.dto.NotificationCountResponse;
import com.manacommunity.sports.dto.NotificationResponse;
import com.manacommunity.sports.model.*;
import com.manacommunity.common.user.repository.AppUserRepository;
import com.manacommunity.sports.repository.CommunityRepository;
import com.manacommunity.sports.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * General-purpose notification service that manages the user-facing
 * notification inbox (table: notification). Separate from the existing
 * SchedulerNotificationService which handles push dispatch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationManagementService {

    private final NotificationRepository notificationRepo;
    private final AppUserRepository      userRepo;
    private final CommunityRepository    communityRepo;

    // ═══════════════════════════════════════════════════════════════
    // CREATE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Creates a notification for a user delivered via both EMAIL and IN_APP channels.
     * Used by email services to persist a record alongside the email dispatch.
     */
    @Transactional
    public Notification createEmailNotification(
            Long userId,
            NotificationType type,
            NotificationCategory category,
            String title,
            String body,
            String actionUrl,
            ReferenceType referenceType,
            Long referenceId,
            NotificationPriority priority
    ) {
        Notification n = Notification.builder()
                .user(userRepo.getReferenceById(userId))
                .type(type)
                .category(category != null ? category : NotificationCategory.GENERAL)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .channels("[\"EMAIL\",\"IN_APP\"]")
                .build();
        n = notificationRepo.save(n);
        log.debug("Email+InApp notification created [{}] type={} for user={}", n.getId(), type, userId);
        return n;
    }

    /**
     * Creates email+in-app notifications for multiple users (bulk).
     */
    @Transactional
    public int createBulkEmailNotifications(
            List<Long> userIds,
            NotificationType type,
            NotificationCategory category,
            String title,
            String body,
            String actionUrl,
            ReferenceType referenceType,
            Long referenceId,
            NotificationPriority priority
    ) {
        List<Notification> notifications = userIds.stream()
                .map(uid -> Notification.builder()
                        .user(userRepo.getReferenceById(uid))
                        .type(type)
                        .category(category != null ? category : NotificationCategory.GENERAL)
                        .title(title)
                        .body(body)
                        .actionUrl(actionUrl)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .priority(priority != null ? priority : NotificationPriority.NORMAL)
                        .channels("[\"EMAIL\",\"IN_APP\"]")
                        .build())
                .toList();
        notificationRepo.saveAll(notifications);
        log.info("Bulk-created {} email notifications of type {} for reference {}:{}",
                notifications.size(), type, referenceType, referenceId);
        return notifications.size();
    }

    /**
     * Creates a single in-app notification for one user.
     */
    @Transactional
    public Notification createNotification(
            Long userId,
            NotificationType type,
            NotificationCategory category,
            String title,
            String body,
            String actionUrl,
            ReferenceType referenceType,
            Long referenceId,
            NotificationPriority priority,
            String metadata,
            Long communityId
    ) {
        Notification n = Notification.builder()
                .user(userRepo.getReferenceById(userId))
                .community(communityId != null ? communityRepo.getReferenceById(communityId) : null)
                .type(type)
                .category(category != null ? category : NotificationCategory.GENERAL)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .priority(priority != null ? priority : NotificationPriority.NORMAL)
                .metadata(metadata)
                .channels("[\"IN_APP\"]")
                .build();
        n = notificationRepo.save(n);
        log.debug("Notification created [{}] type={} for user={}", n.getId(), type, userId);
        return n;
    }

    /**
     * Creates in-app notifications for multiple users (bulk).
     * Used when a schedule is published and all participants need to be notified.
     */
    @Transactional
    public int createBulkNotifications(
            List<Long> userIds,
            NotificationType type,
            NotificationCategory category,
            String title,
            String body,
            String actionUrl,
            ReferenceType referenceType,
            Long referenceId,
            NotificationPriority priority,
            String metadata,
            Long communityId
    ) {
        List<Notification> notifications = userIds.stream()
                .map(uid -> Notification.builder()
                        .user(userRepo.getReferenceById(uid))
                        .community(communityId != null ? communityRepo.getReferenceById(communityId) : null)
                        .type(type)
                        .category(category != null ? category : NotificationCategory.GENERAL)
                        .title(title)
                        .body(body)
                        .actionUrl(actionUrl)
                        .referenceType(referenceType)
                        .referenceId(referenceId)
                        .priority(priority != null ? priority : NotificationPriority.NORMAL)
                        .metadata(metadata)
                        .channels("[\"IN_APP\"]")
                        .build())
                .toList();
        notificationRepo.saveAll(notifications);
        log.info("Bulk-created {} notifications of type {} for reference {}:{}",
                notifications.size(), type, referenceType, referenceId);
        return notifications.size();
    }

    // ═══════════════════════════════════════════════════════════════
    // READ
    // ═══════════════════════════════════════════════════════════════

    public Page<NotificationResponse> getUserNotifications(Long userId, int page, int size) {
        return notificationRepo.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size)
        ).map(this::toResponse);
    }

    public NotificationCountResponse getUnreadCount(Long userId) {
        long count = notificationRepo.countByUserIdAndReadFalseAndDismissedFalse(userId);
        return new NotificationCountResponse(count);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPDATE (read / dismiss)
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public int markAsRead(Long userId, List<Long> notificationIds) {
        return notificationRepo.markAsRead(userId, notificationIds, LocalDateTime.now());
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return notificationRepo.markAllAsRead(userId, LocalDateTime.now());
    }

    @Transactional
    public int dismiss(Long userId, Long notificationId) {
        return notificationRepo.dismiss(userId, notificationId);
    }

    @Transactional
    public int dismissAll(Long userId) {
        return notificationRepo.dismissAll(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // SCHEDULED CLEANUP
    // ═══════════════════════════════════════════════════════════════

    /** Runs daily at 2 AM — auto-dismisses expired notifications. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpired() {
        int dismissed = notificationRepo.dismissExpired(LocalDateTime.now());
        if (dismissed > 0) {
            log.info("Auto-dismissed {} expired notifications", dismissed);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getAnalytics(Long communityId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Object> result = new LinkedHashMap<>();

        long total;
        long totalRead;
        List<Object[]> byType;
        List<Object[]> byCategory;
        List<Object[]> readRateByCategory;
        List<Object[]> daily;
        List<Object[]> byPriority;

        if (communityId != null) {
            total = notificationRepo.countByCommunityId(communityId);
            totalRead = notificationRepo.countByCommunityIdAndReadTrue(communityId);
            byType = notificationRepo.countByCommunityGroupByType(communityId);
            byCategory = notificationRepo.countByCommunityGroupByCategory(communityId);
            readRateByCategory = notificationRepo.readRateByCommunityGroupByCategory(communityId);
            daily = notificationRepo.dailyCountByCommunity(communityId, since);
            byPriority = notificationRepo.countByCommunityGroupByPriority(communityId);
        } else {
            total = notificationRepo.count();
            totalRead = notificationRepo.countByReadTrue();
            byType = notificationRepo.countGroupByType();
            byCategory = notificationRepo.countGroupByCategory();
            readRateByCategory = notificationRepo.readRateGroupByCategory();
            daily = notificationRepo.dailyCount(since);
            byPriority = notificationRepo.countGroupByPriority();
        }

        result.put("totalSent", total);
        result.put("totalRead", totalRead);
        result.put("readRate", total > 0 ? Math.round((double) totalRead / total * 10000.0) / 100.0 : 0);
        result.put("days", days);

        result.put("byType", byType.stream().map(r -> Map.of(
                "type", r[0] != null ? r[0].toString() : "UNKNOWN",
                "count", ((Number) r[1]).longValue()
        )).toList());

        result.put("byCategory", byCategory.stream().map(r -> Map.of(
                "category", r[0] != null ? r[0].toString() : "UNKNOWN",
                "count", ((Number) r[1]).longValue()
        )).toList());

        result.put("readRateByCategory", readRateByCategory.stream().map(r -> {
            long catTotal = ((Number) r[1]).longValue();
            long catRead = ((Number) r[2]).longValue();
            return Map.of(
                    "category", r[0] != null ? r[0].toString() : "UNKNOWN",
                    "total", catTotal,
                    "read", catRead,
                    "readRate", catTotal > 0 ? Math.round((double) catRead / catTotal * 10000.0) / 100.0 : 0
            );
        }).toList());

        result.put("dailyTrend", daily.stream().map(r -> Map.of(
                "date", r[0].toString(),
                "count", ((Number) r[1]).longValue()
        )).toList());

        result.put("byPriority", byPriority.stream().map(r -> Map.of(
                "priority", r[0] != null ? r[0].toString() : "UNKNOWN",
                "count", ((Number) r[1]).longValue()
        )).toList());

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // MAPPING
    // ═══════════════════════════════════════════════════════════════

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType() != null ? n.getType().name() : null,
                n.getCategory() != null ? n.getCategory().name() : null,
                n.getTitle(),
                n.getBody(),
                n.getIcon(),
                n.getActionUrl(),
                n.getReferenceType() != null ? n.getReferenceType().name() : null,
                n.getReferenceId(),
                n.getPriority() != null ? n.getPriority().name() : null,
                n.isRead(),
                n.getReadAt(),
                n.getMetadata(),
                n.getCreatedAt()
        );
    }
}



