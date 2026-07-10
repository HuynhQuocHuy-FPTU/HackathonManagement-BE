package com.hackathon.dto.notification;

import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public record ResponseEntry(Integer senderId,
                            String senderName,
                            String message,
                            LocalDateTime timestamp) {

}
