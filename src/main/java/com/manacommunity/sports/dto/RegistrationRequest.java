package com.manacommunity.sports.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * BUG FIX: RegistrationRequest was a plain class with package-private fields.
 * SportsEventServiceImpl calls req.eventId(); req.categoryId(); etc.
 * Converted to a Java record.
 */
@Data
public class RegistrationRequest {
    @NotNull
    Long eventId;
    @NotNull
    Long categoryId;
    @NotNull
    String matchType;
    String playerName;
    String email;
    String relation;
    String flatNumber;
    Integer age;
    String role;
    Long partnerUserId;

    /**
     * Google reCAPTCHA token from the public registration form. Only verified
     * when {@code app.security.registration.recaptcha.enabled=true}; ignored
     * (may be null) for authenticated/admin add flows.
     */
    String recaptchaToken;
}
