package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
/**
 * Simple DTO returning the unread notification count for badge display.
 */
public record NotificationCountResponse(long unreadCount) {}

