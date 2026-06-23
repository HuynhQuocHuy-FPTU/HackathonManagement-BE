package com.hackathon.service;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.enums.NotificationChannel;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface NotificationService {
    NotificationEmailResponse getInfoNotificationInvite(CustomUserDetails userDetails, Long notificationId);

    void createNotification(Account account,
                            Integer actorId,
                            NotificationType type,
                            NotificationChannel channel,
                            String title,
                            String message
                            );

    void notifyRegistrationApproved(Integer coordinatorId, Account teamLeaderAccount, String teamName, String eventName);

    void notifyRegistrationRejected(Integer coordinatorId, Account teamLeaderAccount, String teamName,String eventName, String reason );

    List<NotificationWebResponse> getNotifications(CustomUserDetails userDetails);
    List<NotificationWebResponse> getUnreadNotifications(CustomUserDetails userDetails);
    List<NotificationWebResponse> getReadNotifications(CustomUserDetails userDetails);
    List<NotificationWebResponse> getByType(CustomUserDetails userDetails, NotificationType type);

    long countUnread(CustomUserDetails userDetails);
    void markAsRead(Long notificationId, CustomUserDetails userDetails);
    void markAllAsRead(CustomUserDetails userDetails);

    void deleteNotification(Long notificationId, CustomUserDetails userDetails);

}
