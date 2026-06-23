package com.hackathon.service;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.enums.InvitationStatus;
import com.hackathon.entity.enums.NotificationChannel;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.NotificationRepository;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    @Override
    public NotificationEmailResponse getInfoNotificationInvite(CustomUserDetails userDetails, Long notificationId) {

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
            if (listNoti.getStatus() == InvitationStatus.ACCEPTED || listNoti.getStatus() == InvitationStatus.REJECTED) {
                throw new BadRequestException("Yêu cầu này đã được xử lý trước đó.");
            }

        } else if (isCoordinator) {
            // Nếu là COORDINATOR: Được quyền xem TẤT CẢ thông báo để hỗ trợ kỹ thuật và kiểm tra hệ thống.
        } else {
            throw new BadRequestException("Tài khoản của bạn không có quyền thực hiện hành động này.");
        }

        return NotificationEmailResponse.builder()
                .notificationId(listNoti.getId())
                .title(listNoti.getTitle())
                .teamName(listNoti.getTeam().getTeamName())
                .message(listNoti.getMessage())
                .type(listNoti.getType())
                .build();
    }

    @Override
    public void createNotification(Account acc, Integer actorId, NotificationType type, NotificationChannel channel, String title, String message) {
        Notification notification = new Notification();

        notification.setAccount(acc);
        notification.setType(type);
        notification.setChannel(channel);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    @Override
    public void notifyRegistrationApproved(Integer coordinatorId, Account teamLeaderAccount, String teamName, String eventName) {
        String title = "Registration Approved";
        String message = "Team của bạn\"" + teamName + "\" đã được phê duyệt tham gia vào cuộc thi " + eventName;

        createNotification(
                teamLeaderAccount,
                coordinatorId,
                NotificationType.TEAM_REGISTRATION_APPROVED,
                NotificationChannel.WEB,
                title,
                message
        );
    }

    @Override
    public void notifyRegistrationRejected(Integer coordinatorId, Account teamLeaderAccount, String teamName,String eventName, String reason ) {
        String title = "Registration Rejected";

        String message = String.format(
                "Your team \"%s\" was rejected in %s. Reason: %s",
                teamName,
                eventName,
                reason
        );

        createNotification(
                teamLeaderAccount,
                coordinatorId,
                NotificationType.TEAM_REGISTRATION_REJECTED,
                NotificationChannel.WEB,
                title,
                message
        );
    }

    @Override
    public List<NotificationWebResponse> getNotifications(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository
                .findByAccount_AccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }


    @Override
    public List<NotificationWebResponse> getUnreadNotifications(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository.findByAccount_AccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NotificationWebResponse> getReadNotifications(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository.findByAccount_AccountIdAndIsReadTrueOrderByCreatedAtDesc(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<NotificationWebResponse> getByType(CustomUserDetails userDetails, NotificationType type) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository
                .findByAccount_AccountIdAndTypeOrderByCreatedAtDesc(accountId, type)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long countUnread(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository.countByAccount_AccountIdAndIsReadFalse(accountId);
    }


    @Override
    public void markAsRead(Long notificationId, CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // check ownership (quan trọng)
        if (notification.getAccount().getAccountId() != accountId) {
            throw new RuntimeException("You cannot modify this notification");
        }

        notification.setRead(true);

        notificationRepository.save(notification);

    }

    @Override
    public void markAllAsRead(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        List<Notification> notifications =
                notificationRepository.findByAccount_AccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId);

        notifications.forEach(n -> n.setRead(true));

        notificationRepository.saveAll(notifications);
    }

    @Override
    public void deleteNotification(Long notificationId, CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getAccount().getAccountId() != accountId) {
            throw new RuntimeException("Forbidden");
        }

        notificationRepository.delete(notification);
    }

    private NotificationWebResponse toResponse(Notification n) {

        return NotificationWebResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .type(n.getType())
                .channel(n.getChannel())
                .build();
    }


}
