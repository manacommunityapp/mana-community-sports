package com.manacommunity.sports.email;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Shared building blocks for the per-domain email services (registration,
 * schedule, match reminder): the common template variables and the formatting
 * helpers. Centralised here so each notifier stays focused on its own flow.
 */
@Component
@RequiredArgsConstructor
public class EmailSupport {

    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final EmailProperties props;

    public EmailProperties props() {
        return props;
    }

    /** Variables every template needs (branding, recipient, footer year). */
    public Map<String, Object> baseVars(String recipientName) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("appName", props.getFromName());
        vars.put("baseUrl", props.getBaseUrl());
        vars.put("recipientName", isBlank(recipientName) ? "there" : recipientName);
        vars.put("year", LocalDate.now().getYear());
        return vars;
    }

    public String formatDate(LocalDate d) {
        return d == null ? "TBA" : d.format(DATE_FMT);
    }

    public String formatTime(LocalDateTime dt) {
        return dt == null ? "TBA" : dt.format(TIME_FMT);
    }

    public String formatDateTime(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATE_FMT) + " " + dt.format(TIME_FMT);
    }

    /** "MIXED_DOUBLES" -> "Mixed Doubles". */
    public String prettify(String enumName) {
        if (isBlank(enumName)) return "";
        String[] parts = enumName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
