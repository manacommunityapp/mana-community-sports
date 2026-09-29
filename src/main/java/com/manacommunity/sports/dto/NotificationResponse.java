package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import java.time.LocalDateTime;

/**
 * Response DTO for a single user notification.
 */
public record NotificationResponse(
    Long id,
    String type,
    String category,
    String title,
    String body,
    String icon,
    String actionUrl,
    String referenceType,
    Long referenceId,
    String priority,
    boolean read,
    LocalDateTime readAt,
    String metadata,
    LocalDateTime createdAt
) {}

