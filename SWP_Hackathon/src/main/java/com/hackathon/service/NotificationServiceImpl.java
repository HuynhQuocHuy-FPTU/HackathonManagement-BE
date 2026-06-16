package com.hackathon.service;

import com.hackathon.dto.notification.NotificationResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.enums.InvitationAction;
import com.hackathon.entity.enums.NotificationStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.NotificationRepository;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    @Override
    public NotificationResponse getInfoNotificationInvite(CustomUserDetails userDetails, Long notificationId) {

        // 1. Xác định loại lời mời đó thuộc trạng thái dì thông qua Id của notificaiton
        Notification listNoti = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin về lời mời."));

//        //2. Check account này có khớp vs account được nhận thông báo không
//        Account currentUser = userDetails.getAccount();
//        if (!currentUser.getEmail().equals(listNoti.getAccount().getEmail())) {
//            throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này, vì tài khoản đăng nhập không hợp lệ");
//        }
        //3. Check lời mời này được  phản hồi chưa
        if (listNoti.getStatus().equals(NotificationStatus.ACCEPTED) || listNoti.getStatus().equals(NotificationStatus.REJECTED)) {
            throw new BadRequestException("Yêu cầu đã được xử lý, bạn không được phép truy cập.");
        }
        //4. Check lời mời còn hạn không

        return NotificationResponse.builder()
                .notificationId(listNoti.getId())
                .title(listNoti.getTitle())
                .teamName(listNoti.getTeam().getTeamName())
                .message(listNoti.getMessage())
                .type(listNoti.getType())
                .build();
    }

}
