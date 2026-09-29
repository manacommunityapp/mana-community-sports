package com.manacommunity.sports.security;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Stateless, dependency-free password-strength evaluator in the spirit of zxcvbn.
 *
 * <p>Composition rules (length + character classes) are handled by
 * {@link PasswordPolicy}. This class focuses on <em>guessability</em>: it blocks
 * the things a complexity check misses — common passwords, dictionary words with
 * trivial leetspeak/affix decoration (e.g. {@code Password123!}, {@code P@ssw0rd}),
 * keyboard walks, sequences, repeats, and passwords derived from the user's own
 * data (email, name, etc.).</p>
 *
 * <p>Produces a 0–4 score (higher is stronger) plus a UI-ready warning and
 * actionable suggestions. If you later want statistical entropy, this can be
 * swapped for a zxcvbn library behind the same {@link Result} shape.</p>
 */
public final class PasswordStrengthEvaluator {

    /** Score below which a password is considered too guessable to accept. */
    public static final int MIN_ACCEPTABLE_SCORE = 3;

    /**
     * The most-abused passwords / base words. Kept deliberately small and
     * high-signal; matching is done against the password's normalised "core"
     * so decorated variants are caught without listing every permutation.
     */
    private static final Set<String> COMMON = Set.of(
            "password", "passw", "pass", "admin", "administrator", "root", "user",
            "welcome", "login", "letmein", "secret", "changeme", "default", "guest",
            "qwerty", "qwertyuiop", "asdf", "asdfgh", "zxcvbn", "qazwsx", "qweasd",
            "iloveyou", "monkey", "dragon", "master", "sunshine", "princess", "football",
            "baseball", "superman", "batman", "trustno", "starwars", "whatever",
            "abc", "abcd", "abcde", "abcdef", "test", "testing", "demo", "sample",
            "manacommunity", "mana", "community", "india", "cricket"
    );

    private static final Set<String> KEYBOARD_WALKS = Set.of(
            "qwerty", "qwertyuiop", "asdfgh", "asdfghjkl", "zxcvbn", "zxcvbnm",
            "qazwsx", "qweasd", "1qaz", "2wsx", "1q2w3e", "1234qwer", "poiuy", "lkjh"
    );

    private static final Pattern YEAR = Pattern.compile("(19|20)\\d{2}");

    private PasswordStrengthEvaluator() {
    }

    /** Outcome of an evaluation. {@code score} is 0 (weakest) … 4 (strongest). */
    public record Result(int score, String warning, List<String> suggestions) {
        public boolean isAcceptable() {
            return score >= MIN_ACCEPTABLE_SCORE;
        }
    }

    public static Result evaluate(String password) {
        return evaluate(password, List.of());
    }

    /**
     * @param password   the candidate password
     * @param userInputs personal tokens to penalise if present (email, name,
     *                   phone, community) — pass what you have; nulls are ignored
     */
    public static Result evaluate(String password, List<String> userInputs) {
        if (password == null || password.isBlank()) {
            return new Result(0, "Password is required.", List.of("Enter a password."));
        }

        final String lower = password.toLowerCase();
        final List<String> suggestions = new ArrayList<>();

        // ── Reward length and character variety ──────────────────────────────
        int len = password.length();
        int score = 0;
        if (len >= 8) score++;
        if (len >= 12) score++;
        if (len >= 16) score++;

        int classes = characterClassCount(password);
        if (classes >= 3) score++;
        score = Math.min(score, 4);

        if (len < 12) suggestions.add("Use 12 or more characters — length matters most.");
        if (classes < 3) suggestions.add("Mix uppercase, lowercase, numbers and symbols.");

        // ── Penalise guessable structure ─────────────────────────────────────
        boolean common = isCommon(lower);
        boolean keyboard = containsKeyboardWalk(lower);
        boolean sequence = hasSequentialRun(lower, 4);
        boolean repeat = hasRepeats(lower);
        boolean year = YEAR.matcher(password).find();
        boolean personal = matchesUserInput(lower, userInputs);

        String warning = null;

        if (common) {
            score = 0;
            warning = "This is a commonly used password and is easy to guess.";
            suggestions.add("Avoid dictionary words and well-known passwords.");
        }
        if (personal) {
            score = Math.min(score, 1);
            warning = warning != null ? warning : "Avoid using your name, email or other personal details.";
            suggestions.add("Don't base your password on personal information.");
        }
        if (keyboard) {
            score = Math.min(score, 1);
            warning = warning != null ? warning : "Keyboard patterns like \"qwerty\" are easy to guess.";
            suggestions.add("Avoid keyboard patterns.");
        }
        if (sequence) {
            score = Math.min(score, 1);
            warning = warning != null ? warning : "Sequences like \"abcd\" or \"1234\" are easy to guess.";
            suggestions.add("Avoid straight sequences of letters or numbers.");
        }
        if (repeat) {
            score = Math.min(score, 2);
            warning = warning != null ? warning : "Repeated characters or patterns weaken a password.";
            suggestions.add("Avoid repeating characters or patterns.");
        }
        if (year) {
            score = Math.min(score, Math.max(score - 1, 0));
            suggestions.add("Avoid years and dates.");
        }

        score = Math.max(0, Math.min(score, 4));

        if (warning == null && score < MIN_ACCEPTABLE_SCORE) {
            warning = "This password is too easy to guess.";
        }
        if (suggestions.isEmpty()) {
            suggestions.add("Consider a passphrase of 4+ unrelated words.");
        }
        return new Result(score, warning, List.copyOf(suggestions));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static int characterClassCount(String pw) {
        boolean upper = false, lower = false, digit = false, special = false;
        for (int i = 0; i < pw.length(); i++) {
            char c = pw.charAt(i);
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else special = true;
        }
        return (upper ? 1 : 0) + (lower ? 1 : 0) + (digit ? 1 : 0) + (special ? 1 : 0);
    }

    /**
     * Checks the password's normalised "core" against the common list, so
     * {@code Password123!} and {@code P@ssw0rd} both resolve to "password".
     */
    private static boolean isCommon(String lower) {
        if (COMMON.contains(lower)) return true;
        String alphaCore = lower.replaceAll("[^a-z]", "");          // strip digits/symbols
        String leetCore = deLeet(lower).replaceAll("[^a-z]", "");   // undo leetspeak first
        return (!alphaCore.isEmpty() && COMMON.contains(alphaCore))
                || (!leetCore.isEmpty() && COMMON.contains(leetCore));
    }

    /** Maps common leetspeak substitutions back to letters for dictionary matching. */
    private static String deLeet(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '0' -> sb.append('o');
                case '1', '!', '|' -> sb.append('i');
                case '3' -> sb.append('e');
                case '4', '@' -> sb.append('a');
                case '5', '$' -> sb.append('s');
                case '7' -> sb.append('t');
                case '8' -> sb.append('b');
                case '9' -> sb.append('g');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean containsKeyboardWalk(String lower) {
        for (String walk : KEYBOARD_WALKS) {
            if (lower.contains(walk)) return true;
        }
        return false;
    }

    /** True if the password contains an ascending/descending run of {@code minRun}+ chars. */
    private static boolean hasSequentialRun(String lower, int minRun) {
        int asc = 1, desc = 1;
        for (int i = 1; i < lower.length(); i++) {
            int diff = lower.charAt(i) - lower.charAt(i - 1);
            asc = (diff == 1) ? asc + 1 : 1;
            desc = (diff == -1) ? desc + 1 : 1;
            if (asc >= minRun || desc >= minRun) return true;
        }
        return false;
    }

    /** True for 3+ identical chars in a row, or a short segment repeated to fill the password. */
    private static boolean hasRepeats(String lower) {
        if (lower.matches(".*(.)\\1\\1.*")) return true; // aaa
        int n = lower.length();
        for (int seg = 1; seg <= n / 2; seg++) {
            if (n % seg == 0) {
                String unit = lower.substring(0, seg);
                if (unit.repeat(n / seg).equals(lower)) return true; // abcabc, xyxyxy
            }
        }
        return false;
    }

    private static boolean matchesUserInput(String lower, List<String> userInputs) {
        if (userInputs == null) return false;
        for (String raw : userInputs) {
            if (raw == null) continue;
            // split emails / full names into tokens and test each meaningful one
            for (String token : raw.toLowerCase().split("[^a-z0-9]+")) {
                if (token.length() >= 3 && lower.contains(token)) return true;
            }
        }
        return false;
    }
}


