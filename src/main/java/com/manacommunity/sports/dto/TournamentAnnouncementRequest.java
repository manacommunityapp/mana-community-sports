package com.manacommunity.sports.dto;

import com.manacommunity.common.enums.*;
import jakarta.validation.constraints.NotBlank;

public record TournamentAnnouncementRequest(
        String template,
        @NotBlank String subject,
        @NotBlank String message,
        boolean sendEmail,
        boolean sendPush,
        String customHtml
) {}

