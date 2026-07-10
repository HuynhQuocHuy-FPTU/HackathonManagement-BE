package com.hackathon.service;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.dto.notification.ResponseEntry;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.enums.NotificationChannel;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.security.CustomUserDetails;

import java.util.List;
import java.util.Set;

public interface NotificationService {
    NotificationEmailResponse getInfoNotificationInvite(CustomUserDetails userDetails, Long notificationId);

    void createNotificationHaveResponse(Account account, Account actor, NotificationType type, NotificationChannel channel, String title, String message, boolean allowResponse, Integer responseDeadline
    );

    void createNotificationNoResponse(Account acc, Account actor, NotificationType type, NotificationChannel channel, String title, String message);

    void notifyRegistrationApproved(Account actor, Account teamLeaderAccount, String teamName, String eventName);

    void notifyRegistrationRejected(Account actor, Account teamLeaderAccount, String teamName, String eventName, String reason);

    void notifyDisqualifyTeam(Account actor, Account teamLeaderAccount, String teamName, String eventName, String reason);

    public void notifyAssignedCategory(Account actor, Account teamLeaderAccount, String teamName, String eventName, String category, Integer responseDeadline);

    void notifyCancelledEvent(Account actor, List<Account> teamLeaderAccounts, String eventName, String reason);

    void notifyCategoryAssignmentResponse(
            Account actor,
            String teamName,
            String responseMessage);

    void responseCategoryAssignment(
            Long notificationId,
            String responseMessage, CustomUserDetails userDetails);

    List<NotificationWebResponse> getNotifications(CustomUserDetails userDetails);

    List<NotificationWebResponse> getUnreadNotifications(CustomUserDetails userDetails);

    List<NotificationWebResponse> getReadNotifications(CustomUserDetails userDetails);

    List<NotificationWebResponse> getByType(CustomUserDetails userDetails, NotificationType type);

    List<ResponseEntry> getPendingResponses(CustomUserDetails userDetails);

    long countUnread(CustomUserDetails userDetails);

    void markAsRead(Long notificationId, CustomUserDetails userDetails);

    void markAllAsRead(CustomUserDetails userDetails);

    void deleteNotification(Long notificationId, CustomUserDetails userDetails);

    //
    void notifyRoundRankingPublished(Account actor, Integer roundId, boolean isFinal);

    void notifyExpertReEvaluation(Account actor, Set<Account> expertsToNotify, String teamName);
}
