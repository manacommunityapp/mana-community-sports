package com.manacommunity.sports.email;

import com.manacommunity.common.util.EmailTemplateRenderer;
import com.manacommunity.common.enums.*;
import java.util.List;

/**
 * Transport-level contract for sending pre-rendered HTML emails.
 * Mirrors the existing {@code PushNotificationService} stub/impl pattern: callers
 * depend on this interface, the wiring decides whether a real SMTP send happens.
 */
public interface EmailService {

    /** Sends one message. Implementations must never throw to the caller — failures are logged. */
    void send(EmailMessage message);

    /** Sends a batch of messages, each independently (one failure does not abort the rest). */
    void sendAll(List<EmailMessage> messages);
}


