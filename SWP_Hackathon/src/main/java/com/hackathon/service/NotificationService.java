package com.hackathon.service;

import com.hackathon.dto.notification.NotificationResponse;
import com.hackathon.security.CustomUserDetails;

public interface NotificationService {
    NotificationResponse getInfoNotificationInvite(CustomUserDetails userDetails, Long notificationId);
}
