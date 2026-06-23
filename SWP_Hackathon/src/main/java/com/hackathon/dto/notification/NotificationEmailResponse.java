package com.hackathon.dto.notification;
import com.hackathon.entity.enums.NotificationType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEmailResponse {
    private Long notificationId;

    private String title;

    private String message;

    private NotificationType type;

    private String teamName;

}
