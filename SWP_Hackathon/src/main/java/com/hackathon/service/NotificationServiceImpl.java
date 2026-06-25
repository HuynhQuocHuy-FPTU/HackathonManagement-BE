package com.hackathon.service;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamMember;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.HackathonEventRepository;
import com.hackathon.repository.NotificationRepository;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;

    private final AccountRepository accountRepository;
    private final HackathonEventRepository eventRepository;

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
    public void createNotificationHaveResponse(Account acc, Account actor, NotificationType type, NotificationChannel channel, String title, String message, boolean allowResponse, Integer responseDeadline ) {
        Notification notification = new Notification();

        notification.setAccount(acc);
        notification.setType(type);
        notification.setChannel(channel);
        notification.setTitle(title);
        notification.setActor(actor);
        notification.setMessage(message);
        notification.setAllowResponse(allowResponse);
        notification.setResponseDeadline(LocalDateTime.now().plusHours(responseDeadline));
        notification.setResponseStatus(NotiResponseStatus.NONE);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void createNotificationNoResponse(Account acc, Account actor, NotificationType type, NotificationChannel channel, String title, String message) {
        Notification notification = new Notification();

        notification.setAccount(acc);
        notification.setType(type);
        notification.setChannel(channel);
        notification.setTitle(title);
        notification.setActor(actor);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyRegistrationApproved(Account actor, Account teamLeaderAccount, String teamName, String eventName) {
        String title = "Registration Approved";
        String message = "Team của bạn\"" + teamName + "\" đã được phê duyệt tham gia vào cuộc thi " + eventName;

        createNotificationNoResponse(
                teamLeaderAccount,
                actor,
                NotificationType.TEAM_REGISTRATION_APPROVED,
                NotificationChannel.WEB,
                title,
                message
        );
    }

    @Override
    public void notifyRegistrationRejected(Account actor, Account teamLeaderAccount, String teamName,String eventName, String reason ) {
        String title = "Registration Rejected";

        String message = String.format(
                "Đội của bạn \"%s\" đã bị từ chối đăng kí tham gia cuộc thi %s. Lý do: %s",
                teamName,
                eventName,
                reason
        );

        createNotificationNoResponse(
                teamLeaderAccount,
                actor,
                NotificationType.TEAM_REGISTRATION_REJECTED,
                NotificationChannel.WEB,
                title,
                message
        );
    }

    @Override
    public void notifyDisqualifyTeam(Account actor, Account teamLeaderAccount, String teamName, String eventName, String reason) {
        String title = "Loại team tham gia khỏi cuộc thi";

        String message = String.format(
                "Team của bạn \"%s\" đã bị loại khỏi cuộc thi %s. Lý do: %s",
                teamName,
                eventName,
                reason
        );

        createNotificationNoResponse(
                teamLeaderAccount,
                actor,
                NotificationType.DISQUALIFY_TEAM,
                NotificationChannel.WEB,
                title,
                message);
    }

    @Override
    public void notifyAssignedCategory(Account actor, Account teamLeaderAccount, String teamName, String eventName, String category, Integer responseDeadline) {
        String title = "Hạng mục tham gia";

        String message = String.format(
                """
                Team "%s" đã được phân vào hạng mục "%s" của cuộc thi "%s".
    
                Vui lòng kiểm tra lại thông tin. Nếu có sai sót, bạn có thể gửi phản hồi trong vòng "%s" tiếng kể từ thời điểm nhận thông báo.
                """,
                teamName,
                category,
                eventName,
                responseDeadline
        );

        createNotificationHaveResponse(
                teamLeaderAccount,
                actor,
                NotificationType.ASSIGNED_CATEGORY,
                NotificationChannel.WEB,
                title,
                message,
                true,
                responseDeadline
        );
    }

    @Override
    @Transactional
    public void notifyCancelledEvent(Account actor, List<Account> teamLeaderAccounts, String eventName, String reason) {
        String title = "Thông báo hủy sự kiện";

        String message = String.format(
                "Rất tiếc, cuộc thi \"%s\" đã bị hủy bỏ. Lý do: %s",
                eventName,
                reason
        );

        // Gửi thông báo đến từng Team Leader trong danh sách
        for (Account leaderAccount : teamLeaderAccounts) {
            createNotificationNoResponse(
                    leaderAccount,
                    actor,
                    NotificationType.CANCELLED_EVENT,
                    NotificationChannel.WEB,
                    title,
                    message
            );
        }
    }

    @Override
    @Transactional
    public void notifyCategoryAssignmentResponse(Account actor, String teamName, String responseMessage) {
        String title = "Category Assignment Feedback";

        String message = String.format(
                """
                Leader của team "%s" đã gửi phản hồi về kết quả phân category.
    
                Nội dung phản hồi:
                "%s"
                """,
                teamName,
                responseMessage
        );
        List<Account> coordinators = accountRepository.findAccountByRole(AccountRole.EVENTCOORDINATOR);

        for(Account acc : coordinators){
            //gửi thông báo cho toàn bộ coordinator
            createNotificationNoResponse(
                    acc,
                    actor,
                    NotificationType.ASSIGNED_CATEGORY,
                    NotificationChannel.WEB,
                    title,
                    message
            );
        }

    }
    @Override
    @Transactional
    public void responseCategoryAssignment(
            Long notificationId,
            String responseMessage,CustomUserDetails userDetails) {
        Account acc = userDetails.getAccount();
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new BadRequestException("Notification không tồn tại"));
        if (LocalDateTime.now()
                .isAfter(notification.getResponseDeadline())) {
            throw new BadRequestException(
                    "Đã hết thời gian phản hồi");
        }
        if (!notification.isAllowResponse()) {
            throw new BadRequestException(
                    "Thông báo này không cho phép phản hồi");
        }

        if(notification != null){
            if(notification.getResponseStatus() == NotiResponseStatus.NONE){
                notification.setResponseMessage(responseMessage);
                System.out.println(notification.getResponseMessage());
                notification.setResponseAt(LocalDateTime.now());
                notification.setResponseStatus(NotiResponseStatus.PENDING);
                System.out.println(notification.getResponseStatus());
                notification = notificationRepository.saveAndFlush(notification);
            }else{
                throw new BadRequestException("Thông báo này đã được phản hồi");
            }

        }


        // lấy ra team của leader đang phản hồi
        Team team = notification.getAccount().getStudent().getTeamMembers().stream().map(TeamMember::getTeam).findFirst().orElseThrow(() -> new BadRequestException("Không tìm thấy Team của leader"));

        this.notifyCategoryAssignmentResponse(
                acc,
                team.getTeamName(),
                responseMessage);
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
    public List<NotificationWebResponse> getPendingResponses(CustomUserDetails userDetails) {
        List<Notification> list =  notificationRepository.findNotificationByAccount_AccountIdAndResponseStatus(userDetails.getAccount().getAccountId(), NotiResponseStatus.PENDING);

         return list.stream().map(this::toResponse).toList();
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
                .allowResponse(n.isAllowResponse())
                .build();
    }


}
