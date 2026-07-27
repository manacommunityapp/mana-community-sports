package com.manacommunity.sports.repository;

import com.manacommunity.sports.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Paginated feed: all non-dismissed notifications for a user, newest first. */
    Page<Notification> findByUserIdAndDismissedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // ── Analytics queries ─────────────────────────────────────────────────

    long countByCommunityId(Long communityId);

    long countByCommunityIdAndReadTrue(Long communityId);

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.community.id = :cid GROUP BY n.type ORDER BY COUNT(n) DESC")
    List<Object[]> countByCommunityGroupByType(@Param("cid") Long communityId);

    @Query("SELECT n.category, COUNT(n) FROM Notification n WHERE n.community.id = :cid GROUP BY n.category ORDER BY COUNT(n) DESC")
    List<Object[]> countByCommunityGroupByCategory(@Param("cid") Long communityId);

    @Query("SELECT n.category, COUNT(n), SUM(CASE WHEN n.read = true THEN 1 ELSE 0 END) FROM Notification n WHERE n.community.id = :cid GROUP BY n.category")
    List<Object[]> readRateByCommunityGroupByCategory(@Param("cid") Long communityId);

    @Query("SELECT CAST(n.createdAt AS date), COUNT(n) FROM Notification n WHERE n.community.id = :cid AND n.createdAt >= :since GROUP BY CAST(n.createdAt AS date) ORDER BY CAST(n.createdAt AS date)")
    List<Object[]> dailyCountByCommunity(@Param("cid") Long communityId, @Param("since") LocalDateTime since);

    @Query("SELECT n.priority, COUNT(n) FROM Notification n WHERE n.community.id = :cid GROUP BY n.priority")
    List<Object[]> countByCommunityGroupByPriority(@Param("cid") Long communityId);

    // Global analytics (super-admin)

    @Query("SELECT n.type, COUNT(n) FROM Notification n GROUP BY n.type ORDER BY COUNT(n) DESC")
    List<Object[]> countGroupByType();

    @Query("SELECT n.category, COUNT(n) FROM Notification n GROUP BY n.category ORDER BY COUNT(n) DESC")
    List<Object[]> countGroupByCategory();

    @Query("SELECT n.category, COUNT(n), SUM(CASE WHEN n.read = true THEN 1 ELSE 0 END) FROM Notification n GROUP BY n.category")
    List<Object[]> readRateGroupByCategory();

    @Query("SELECT CAST(n.createdAt AS date), COUNT(n) FROM Notification n WHERE n.createdAt >= :since GROUP BY CAST(n.createdAt AS date) ORDER BY CAST(n.createdAt AS date)")
    List<Object[]> dailyCount(@Param("since") LocalDateTime since);

    @Query("SELECT n.priority, COUNT(n) FROM Notification n GROUP BY n.priority")
    List<Object[]> countGroupByPriority();

    long countByReadTrue();

    /** Unread badge count. */
    long countByUserIdAndReadFalseAndDismissedFalse(Long userId);

    /** Find by polymorphic reference (e.g., all notifications for a specific tournament config). */
    List<Notification> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);

    /** Mark specific notification IDs as read for a user. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.user.id = :userId AND n.id IN :ids")
    int markAsRead(@Param("userId") Long userId, @Param("ids") List<Long> ids, @Param("now") LocalDateTime now);

    /** Mark ALL unread notifications as read for a user. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.readAt = :now WHERE n.user.id = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Dismiss a single notification. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.dismissed = true WHERE n.user.id = :userId AND n.id = :id")
    int dismiss(@Param("userId") Long userId, @Param("id") Long id);

    /** Dismiss all notifications for a user. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.dismissed = true WHERE n.user.id = :userId")
    int dismissAll(@Param("userId") Long userId);

    /** Cleanup: auto-dismiss expired notifications. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.dismissed = true WHERE n.expiresAt IS NOT NULL AND n.expiresAt < :now AND n.dismissed = false")
    int dismissExpired(@Param("now") LocalDateTime now);
}
