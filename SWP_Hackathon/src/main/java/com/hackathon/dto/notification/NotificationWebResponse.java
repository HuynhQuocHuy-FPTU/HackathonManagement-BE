package com.hackathon.dto.notification;

import com.hackathon.entity.enums.NotificationChannel;
import com.hackathon.entity.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationWebResponse {
    private Long id;

    private String title;

    private String message;

    private boolean isRead;

    private LocalDateTime createdAt;

    private NotificationType type;

    private NotificationChannel channel;

    private boolean allowResponse;

}
