package com.manacommunity.sports.email;

/**
 * The set of transactional emails this application sends — the full Sports
 * Tournament email flow. Each value maps to a Thymeleaf template under
 * {@code classpath:/templates/email/} and a default subject line.
 */
public enum EmailTemplate {

    /** Player just submitted a registration (status REGISTERED). */
    REGISTRATION_RECEIVED("registration-received", "We received your registration", EmailCategory.REGISTRATION,
            new Trigger("Sports → Register", true,
                    "A player submits a registration for an event that requires admin approval; the entry lands PENDING.")),

    /** A registration was confirmed by the organiser (status CONFIRMED). */
    REGISTRATION_CONFIRMED("registration-confirmed", "You're confirmed!", EmailCategory.REGISTRATION,
            new Trigger("Sports → Register · Sports → Admin → Registrations", true,
                    "The registration auto-confirms (event doesn't require approval), or an admin confirms a PENDING entry.")),

    /** A registration was not approved (status REJECTED / WITHDRAWN by organiser). */
    REGISTRATION_REJECTED("registration-rejected", "Update on your registration", EmailCategory.REGISTRATION,
            new Trigger("Sports → Admin → Registrations", true,
                    "An admin rejects a pending registration, optionally with a reason.")),

    /** A tournament's match schedule was published. */
    SCHEDULE_PUBLISHED("schedule-published", "Match schedule is live", EmailCategory.TOURNAMENT,
            new Trigger("Sports → Schedule", true,
                    "An admin publishes the match schedule/fixtures for a tournament; confirmed participants are notified.")),

    /** A participant's own match is about to start. */
    MATCH_REMINDER("match-reminder", "Your match starts soon", EmailCategory.MATCH,
            new Trigger("Sports → Auction (team rosters) · Sports → Schedule (match time)", true,
                    "Automatic background job — fires shortly before a published match's scheduled start, to that match's two team rosters (owner, captain, players).")),

    /** A player won their match / advanced a round. */
    WINNER_NOTIFICATION("winner-notification", "You won your match! 🎉", EmailCategory.MATCH,
            new Trigger("Sports → Schedule (bracket / match result entry)", true,
                    "An admin records a completed match result; the winning side's players are emailed automatically.")),

    /** The tournament finished — champion, runner-up and third place. */
    TOURNAMENT_COMPLETION("tournament-completion", "Tournament results are in", EmailCategory.TOURNAMENT,
            new Trigger("Sports → Schedule (bracket / match result entry)", true,
                    "Fires automatically to all confirmed participants once the FINAL round's result is recorded (champion, runner-up, third place).")),

    /** Prize distribution / closing ceremony details. */
    PRIZE_DISTRIBUTION("prize-distribution", "Prize distribution details", EmailCategory.PRIZE,
            new Trigger(null, false,
                    "Not currently wired to any UI action or automatic job — defined for a future prize-ceremony flow. Reachable only from this admin test-send tool.")),

    /** Tournament created — announcing registration is open. */
    TOURNAMENT_OPEN("tournament-open", "Registration is now open!", EmailCategory.TOURNAMENT,
            new Trigger("Sports → Admin → Tournament management → \"Notify Registration Open\"", true,
                    "An admin clicks \"Notify Registration Open\" for a tournament (or picks this template in the Announcement composer).")),

    /** General tournament announcement (custom or pre-built). */
    TOURNAMENT_ANNOUNCEMENT("tournament-announcement", "Tournament announcement", EmailCategory.ANNOUNCEMENT,
            new Trigger("Sports → Admin → Tournament management → Announcement", true,
                    "Default template for the admin's free-form tournament announcement composer (venue changes, general updates, etc.).")),

    /** One-time passcode emailed to verify an address before registration. */
    EMAIL_OTP("email-otp", "Your verification code", EmailCategory.AUTH,
            new Trigger("Sports → Register (event registration form)", true,
                    "A player requests an email verification code before submitting a sports event registration.")),

    /** Tournament registrations are now open for the community. */
    REGISTRATION_OPEN("registration-open", "Registrations are now open!", EmailCategory.REGISTRATION,
            new Trigger("Sports → Admin → Tournament management → Update Status", true,
                    "Sent automatically to all participants when an admin changes a tournament's status to \"Registration Open.\"")),

    /** Tournament has officially started / opening ceremony. */
    TOURNAMENT_START("tournament-start", "Welcome to the tournament! 🚀", EmailCategory.TOURNAMENT,
            new Trigger(null, false,
                    "Not currently wired to any UI action or automatic job — defined for a future opening-ceremony flow. Reachable only from this admin test-send tool."));

    private final String templateName;
    private final String defaultSubject;
    private final EmailCategory category;
    private final Trigger trigger;

    EmailTemplate(String templateName, String defaultSubject, EmailCategory category, Trigger trigger) {
        this.templateName = templateName;
        this.defaultSubject = defaultSubject;
        this.category = category;
        this.trigger = trigger;
    }

    /** Thymeleaf logical name, e.g. {@code email/registration-received}. */
    public String templateName() {
        return "email/" + templateName;
    }

    public String defaultSubject() {
        return defaultSubject;
    }

    /**
     * Broad grouping used by admin tooling to style/order templates without needing
     * a per-template lookup table that has to be updated by hand every time a new
     * template is added.
     */
    public EmailCategory category() {
        return category;
    }

    /** Where/how this email actually gets sent in the live app — surfaced in admin tooling. */
    public Trigger trigger() {
        return trigger;
    }

    public enum EmailCategory {
        REGISTRATION, TOURNAMENT, MATCH, PRIZE, ANNOUNCEMENT, AUTH
    }

    /**
     * @param menuPath human-readable module/menu/submenu path where this fires, or
     *                 {@code null} when the template isn't wired to any live trigger yet
     * @param wired    false when nothing in the app currently calls this template
     * @param description what specifically causes the send
     */
    public record Trigger(String menuPath, boolean wired, String description) {}
}
