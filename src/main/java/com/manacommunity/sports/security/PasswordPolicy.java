package com.manacommunity.sports.security;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import com.manacommunity.common.exception.ManaCommunityException;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Centralised password-strength rules (no server-side state).
 *
 * <p>Requirements enforced: minimum 8 characters and at least one each of
 * uppercase, lowercase, digit and special character, <em>and</em> a
 * guessability gate via {@link PasswordStrengthEvaluator} that blocks common
 * passwords, keyboard walks, sequences and personal-data-based passwords
 * (e.g. {@code Password123!}). 12+ characters is recommended.</p>
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int RECOMMENDED_LENGTH = 12;
    private static final String SPECIALS = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`\"\\";

    private PasswordPolicy() {
    }

    /**
     * Throws {@link ManaCommunityException} (400 WEAK_PASSWORD) with a clear, UI-ready
     * message when {@code raw} fails any hard requirement or is too guessable.
     */
    public static void validate(String raw) {
        validate(raw, List.of());
    }

    /**
     * As {@link #validate(String)}, but also penalises passwords derived from the
     * caller's own data (email, name, phone, community).
     *
     * @param userInputs personal tokens to reject if present; may be empty
     */
    public static void validate(String raw, List<String> userInputs) {
        if (raw == null || raw.isBlank()) {
            throw weak("Password is required.");
        }
        if (raw.length() < MIN_LENGTH) {
            throw weak("Password must be at least " + MIN_LENGTH + " characters long.");
        }
        boolean upper = false, lower = false, digit = false, special = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else if (SPECIALS.indexOf(c) >= 0) special = true;
        }
        StringBuilder missing = new StringBuilder();
        if (!upper) missing.append("an uppercase letter, ");
        if (!lower) missing.append("a lowercase letter, ");
        if (!digit) missing.append("a number, ");
        if (!special) missing.append("a special character, ");
        if (missing.length() > 0) {
            missing.setLength(missing.length() - 2); // trim trailing ", "
            throw weak("Password must include " + missing + ".");
        }

        // Guessability gate: blocks common/dictionary/keyboard/personal passwords
        // that pass the composition rules above (e.g. "Password123!").
        PasswordStrengthEvaluator.Result strength = PasswordStrengthEvaluator.evaluate(raw, userInputs);
        if (!strength.isAcceptable()) {
            String msg = strength.warning() != null ? strength.warning() : "This password is too easy to guess.";
            if (!strength.suggestions().isEmpty()) {
                msg = msg + " " + strength.suggestions().get(0);
            }
            throw weak(msg);
        }
    }

    /** Non-blocking advisory label ("weak"/"fair"/"good"/"strong"/"very strong") for UI display. */
    public static String strengthHint(String raw) {
        return switch (PasswordStrengthEvaluator.evaluate(raw).score()) {
            case 0 -> "weak";
            case 1 -> "fair";
            case 2 -> "good";
            case 3 -> "strong";
            default -> "very strong";
        };
    }

    private static ManaCommunityException weak(String message) {
        return new ManaCommunityException(message, HttpStatus.BAD_REQUEST, "WEAK_PASSWORD");
    }
}



