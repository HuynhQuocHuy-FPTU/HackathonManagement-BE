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

        // 2. Kiểm tra Role của người dùng hiện tại
        boolean isStudent = userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_STUDENT"));

        boolean isCoordinator = userDetails.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_COORDINATOR"));

        Account currentUser = userDetails.getAccount();
        // 3. kiểm tra quyền truy cập
        if (isStudent) {
            // Nếu là STUDENT: Bắt buộc tài khoản nhận thông báo phải khớp với tài khoản đang đăng nhập
            if (!currentUser.getEmail().equals(listNoti.getAccount().getEmail())) {
                throw new BadRequestException("Bạn không có quyền truy cập vào thông báo này.");
            }

            // Nếu là STUDENT: Lời mời đã xử lý thì không cho vào nữa để tránh bấm lại
            if (listNoti.getStatus() == NotificationStatus.ACCEPTED || listNoti.getStatus() == NotificationStatus.REJECTED) {
                throw new BadRequestException("Yêu cầu này đã được xử lý trước đó.");
            }

        } else if (isCoordinator) {
            // Nếu là COORDINATOR: Được quyền xem TẤT CẢ thông báo để hỗ trợ kỹ thuật và kiểm tra hệ thống.
        } else {
            throw new BadRequestException("Tài khoản của bạn không có quyền thực hiện hành động này.");
        }

        return NotificationResponse.builder()
                .notificationId(listNoti.getId())
                .title(listNoti.getTitle())
                .teamName(listNoti.getTeam().getTeamName())
                .message(listNoti.getMessage())
                .type(listNoti.getType())
                .build();
    }

}
