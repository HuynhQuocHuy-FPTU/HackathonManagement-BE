package com.hackathon.service;

import com.hackathon.entity.Notification;
import com.hackathon.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private EmailService emailService;

    public void sendInvitation(){

        Notification noti = new Notification();
        noti.setTitle("00");
        noti.setMessage("");
        notificationRepository.save(noti);
    }
}
