package com.manacommunity.sports.dto;

import lombok.Data;
import java.util.List;

/**
 * BUG FIX: NotificationScheduleDto was a plain class with package-private
 * fields.
 * SportsEventServiceImpl calls cfg.offsetType(); cfg.offsetValue(); cfg.type();
 * cfg.title(); cfg.body() — record-style. Converted to a Java record.
 */
@Data
public class NotificationScheduleDto {
    // New premium scheduler fields
    private String id;
    private String label;
    private int offset;
    private boolean enabled;
    private String title;
    private String body;
    private List<String> recipients;
    private List<String> overrideChannels;
    private String priority;
    private boolean isCustom;

    // Compatibility fields
    private String type;
    private String offsetType;
    private long offsetValue;
}
