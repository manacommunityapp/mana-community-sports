package com.manacommunity.sports.security;

/**
 * Centralised masking for sensitive values so they never reach logs in clear text.
 *
 * <p>Covers the data classes the logging framework must never persist:
 * passwords, OTPs, JWT/refresh tokens, Aadhaar, bank data, and PII such as
 * email/mobile. Always mask at the edge (filters, audit, exception handlers)
 * rather than relying on call sites to remember.</p>
 *
 * <p>All methods are null/blank safe and never throw.</p>
 */
public final class MaskingUtil {

    /** Fixed placeholder for values that must be fully redacted (passwords, OTPs, tokens, bank data). */
    public static final String REDACTED = "***";

    private MaskingUtil() {
    }

    /** Email → keeps the first two local-part chars and the domain: {@code sa*****@gmail.com}. */
    public static String maskEmail(String email) {
        if (isBlank(email)) return "-";
        int at = email.indexOf('@');
        if (at < 0) {
            return maskKeepingEnds(email, 2, 0);
        }
        String local = email.substring(0, at);
        String domain = email.substring(at); // includes '@'
        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "*"
                : local.substring(0, 2) + "*".repeat(Math.min(local.length() - 2, 5));
        return maskedLocal + domain;
    }

    /** Mobile → keeps first two and last two digits: {@code 98******21}. */
    public static String maskMobile(String mobile) {
        if (isBlank(mobile)) return "-";
        String digits = mobile.replaceAll("\\D", "");
        if (digits.length() < 4) return REDACTED;
        return maskKeepingEnds(digits, 2, 2);
    }

    /** Aadhaar / long identifiers → keeps only the last four: {@code ********1234}. */
    public static String maskAadhaar(String aadhaar) {
        if (isBlank(aadhaar)) return "-";
        String digits = aadhaar.replaceAll("\\D", "");
        if (digits.length() <= 4) return REDACTED;
        return "*".repeat(digits.length() - 4) + digits.substring(digits.length() - 4);
    }

    /** Tokens / secrets → never reveal. Shows a short, non-reversible prefix for correlation only. */
    public static String maskToken(String token) {
        if (isBlank(token)) return "-";
        return token.length() <= 6 ? REDACTED : token.substring(0, 4) + "…" + REDACTED;
    }

    /** Fully redact a value (passwords, OTPs, bank data). */
    public static String redact(String value) {
        return isBlank(value) ? "-" : REDACTED;
    }

    /** Generic masker keeping {@code visibleStart} leading and {@code visibleEnd} trailing chars. */
    public static String maskKeepingEnds(String value, int visibleStart, int visibleEnd) {
        if (isBlank(value)) return "-";
        int len = value.length();
        if (len <= visibleStart + visibleEnd) return REDACTED;
        return value.substring(0, visibleStart)
                + "*".repeat(len - visibleStart - visibleEnd)
                + value.substring(len - visibleEnd);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
