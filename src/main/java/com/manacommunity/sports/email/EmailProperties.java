package com.manacommunity.sports.email;

import com.manacommunity.common.util.EmailTemplateRenderer;
import com.manacommunity.common.enums.*;
import com.manacommunity.common.model.Community;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code app.mail.*} settings that drive the email-notification layer.
 * Kept separate from Spring's own {@code spring.mail.*} (SMTP transport) so the
 * application can decide <em>whether</em> to send independently of how it connects.
 */
@Component
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class EmailProperties {

    /** Master switch. When false, emails are rendered + logged but never dispatched. */
    private boolean enabled = false;

    /** Envelope "from" address. */
    private String from = "no-reply@manacommunity.app";

    /** Friendly display name shown next to the from address. */
    private String fromName = "Mana Community";

    /** Public base URL of the React app — used to build action links inside emails. */
    private String baseUrl = "http://localhost:5173";

    /** How many minutes before a match starts the self-match reminder is sent. */
    private int matchReminderLeadMinutes = 30;

    /**
     * When set, controls where notification emails are delivered.
     * <ul>
     *   <li>{@code NONE} (default) — send to the real recipient (production mode)</li>
     *   <li>{@code REDIRECT} — send to {@link #defaultRecipient} instead of the real
     *       recipient; useful for dev/staging so no real user gets mail</li>
     *   <li>{@code CC} — send to the real recipient AND copy {@link #defaultRecipient}</li>
     * </ul>
     */
    private RecipientMode recipientMode = RecipientMode.NONE;

    /** Default recipient address used when {@link #recipientMode} is REDIRECT or CC. */
    private String defaultRecipient;

    public enum RecipientMode { NONE, REDIRECT, CC }
}



