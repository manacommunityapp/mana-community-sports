package com.manacommunity.sports.model;

/**
 * Types of user-facing notifications stored in the notification table.
 */
public enum NotificationType {
    // Sports — schedule & match lifecycle
    SCHEDULE_PUBLISHED,
    SCHEDULE_UPDATED,
    MATCH_REMINDER,
    MATCH_RESULT_POSTED,

    // Registration lifecycle
    REGISTRATION_RECEIVED,
    REGISTRATION_OPEN,
    REGISTRATION_CONFIRMED,
    REGISTRATION_REJECTED,
    REGISTRATION_WITHDRAWN,

    // Event lifecycle
    EVENT_UPDATED,
    EVENT_CANCELLED,
    EVENT_STATUS_CHANGED,

    // Auction lifecycle
    AUCTION_STARTED,
    AUCTION_COMPLETED,
    PLAYER_SOLD,
    BID_OUTBID,

    // Team & captain
    TEAM_ASSIGNED,
    TEAM_CREATED,
    CAPTAIN_NOMINATED,
    CAPTAIN_CONFIRMED,

    // Tournament results
    WINNER_NOTIFICATION,
    TOURNAMENT_COMPLETED,
    PRIZE_DISTRIBUTION,

    // Tournament announcements
    TOURNAMENT_OPEN,
    TOURNAMENT_ANNOUNCEMENT,

    // General
    GENERAL,

    // Visitor
    VISITOR_PENDING,
    VISITOR_CHECK_IN
}
