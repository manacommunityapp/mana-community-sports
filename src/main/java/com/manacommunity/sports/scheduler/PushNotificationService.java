package com.manacommunity.sports.scheduler;

import com.manacommunity.common.enums.*;
import com.manacommunity.common.user.model.AppUser;
import java.util.List;

/**
 * BUG FIX: PushNotificationService was referenced in NotificationScheduler
 * but never defined. Provide an interface so Spring can wire it and
 * developers can implement it (Firebase, OneSignal, etc.).
 */
public interface PushNotificationService {
    void sendBulk(List<AppUser> recipients, String title, String body);
}


