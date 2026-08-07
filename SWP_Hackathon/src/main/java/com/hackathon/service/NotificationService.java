package com.hackathon.service;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.dto.notification.ResponseEntry;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.NotificationChannel;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.entity.enums.RequestType;
import com.hackathon.security.CustomUserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface NotificationService {
    NotificationEmailResponse getInfoNotificationInvite(CustomUserDetails userDetails, Long notificationId);

    void createNotificationHaveResponse(Account account, Account actor, Round round, NotificationType type, NotificationChannel channel, String title, String message, boolean allowResponse, Integer responseDeadline
    );

    void createNotificationNoResponse(Account acc, Account actor, NotificationType type, NotificationChannel channel, String title, String message);

    void notifyRegistrationApproved(Account actor, Account teamLeaderAccount, String teamName, String eventName);

    void notifyRegistrationRejected(Account actor, Account teamLeaderAccount, String teamName, String eventName, String reason);

    void notifyDisqualifyTeam(Account actor, Account teamLeaderAccount, String teamName, String eventName, String reason);

    void notifyAssignedCategory(
            Account actor,
            Account teamLeaderAccount,
            Team team,
            Round round,
            String eventName,
            String category,
            Integer responseDeadline,
            String oldCategory
    );

    void notifyAssignedCategoryFinal(Account actor, Account teamLeaderAccount, String teamName,
                                     String eventName, String category, String oldCategory);

    void notifyCancelledEvent(Account actor, List<Account> teamLeaderAccounts, String eventName, String reason);

    void notifyAutoCancelledEventCoordinator(Account coordinatorAccount, String eventName, String reason);

    void notifyTeamRequestResolved(
            Account actor,
            Account teamLeaderAccount,
            String teamName,
            RequestType requestType
    );

    void checkResponseNoti(Long notificationId);

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
    void notifyRoundRankingPublished(Account actor, Integer roundId, boolean isFinal, Integer responseDeadline);

    void notifyScoringFailureToAllCoordinators(Round round, String reason);

    void notifyScoringCompletedToAllCoordinators(Round round);

    void notifyExpertReEvaluation(Account actor, Set<Account> expertsToNotify, String teamName);

    void notifyResponseAppeal(Account actor, Account account, String teamName, boolean isChanged);

    void notifyMentorSupportTeam(Account teamLeader, List<ExpertAssign> mentors);

    void notifyInviteTeam(Account teamLeader, Account account, String type, Long invitationId);

    void notifyMentorResponseSupportTeam(Account teamLeader, Account mentor , String responseMessage,boolean isAccepted);
}
