package com.hackathon.dto.notification;

import com.hackathon.entity.enums.InvitationAction;
import com.hackathon.entity.enums.NotificationStatus;
import com.hackathon.entity.enums.NotificationType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private Long notificationId;

    private String title;

    private String message;

    private NotificationType type;

//    private NotificationStatus status;

    private String teamName;

//    private InvitationAction action;
}
