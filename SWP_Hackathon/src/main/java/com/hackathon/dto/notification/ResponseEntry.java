package com.hackathon.dto.notification;

import java.time.LocalDateTime;

public record ResponseEntry(Long senderId,
                            String senderName,
                            String message,
                            LocalDateTime timestamp) {

}
