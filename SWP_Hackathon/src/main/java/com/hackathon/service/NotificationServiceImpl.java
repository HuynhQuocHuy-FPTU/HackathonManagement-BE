package com.hackathon.service;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.notification.NotificationWebResponse;
import com.hackathon.dto.notification.ResponseEntry;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final RoundRepository roundRepository;
    private final EmailService emailService;

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
    public void createNotificationHaveResponse(Account acc, Account actor, NotificationType type, NotificationChannel channel, String title, String message, boolean allowResponse, Integer responseDeadline) {
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
    public void notifyTeamRequestResolved(
            Account actor,
            Account teamLeaderAccount,
            String teamName,
            RequestType requestType
    ) {
        NotificationType notificationType;
        String title;
        String message;

        switch (requestType) {
            case APPEAL -> {
                notificationType = NotificationType.RANKING_DRAFT;
                title = "Kết quả giải quyết khiếu nại điểm";
                message = "Yêu cầu khiếu nại điểm của đội \""
                        + teamName
                        + "\" đã được giải quyết. Vui lòng kiểm tra lại kết quả.";
            }
            case DRAW_RESULT_VERIFICATION -> {
                notificationType = NotificationType.ASSIGNED_CATEGORY;
                title = "Kết quả giải quyết yêu cầu xác thực";
                message = "Yêu cầu xác thực kết quả bốc thăm của đội \""
                        + teamName
                        + "\" đã được giải quyết. Vui lòng kiểm tra lại hạng mục.";
            }
            default -> throw new BadRequestException(
                    "Loại yêu cầu này không hỗ trợ notification kết quả");
        }

        createNotificationNoResponse(
                teamLeaderAccount,
                actor,
                notificationType,
                NotificationChannel.WEB,
                title,
                message
        );
    }

    @Override
    public void notifyRegistrationApproved(Account actor, Account teamLeaderAccount, String teamName, String eventName) {
        String title = "Chấp nhận đơn đăng ký tham gia cuộc thi";
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
    public void notifyRegistrationRejected(Account actor, Account teamLeaderAccount, String teamName, String eventName, String reason) {
        String title = "Từ chối đơn đăng kí tham gia cuộc thi";

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
    public void notifyAssignedCategory(
            Account actor,
            Account teamLeaderAccount,
            Team team,
            Round round,
            String eventName,
            String category,
            Integer responseDeadline,
            String oldCategory
    ) {
        String title = "Hạng mục tham gia";

        String message = String.format(
                """
                        Team "%s" đã được phân vào hạng mục "%s" của cuộc thi "%s".
                        
                        Vui lòng kiểm tra lại thông tin. Nếu có sai sót, bạn có thể gửi phản hồi trong vòng "%s" tiếng kể từ thời điểm nhận thông báo.
                        """,
                team.getTeamName(),
                category,
                eventName,
                responseDeadline
        );

        Notification notification = new Notification();
        notification.setAccount(teamLeaderAccount);
        notification.setActor(actor);
        notification.setTeam(team);
        notification.setRound(round);
        notification.setType(NotificationType.ASSIGNED_CATEGORY);
        notification.setChannel(NotificationChannel.WEB);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setAllowResponse(true);
        notification.setResponseDeadline(
                LocalDateTime.now().plusHours(responseDeadline));
        notification.setResponseStatus(NotiResponseStatus.NONE);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    public void notifyAssignedCategoryFinal(Account actor, Account teamLeaderAccount, String teamName,
                                            String eventName, String category, String oldCategory) {
        String title = "Kết quả xác thực hạng mục";
        String message = String.format(
                "Yêu cầu của team \"%s\" đã được xử lý. Hạng mục được cập nhật từ \"%s\" sang \"%s\" trong cuộc thi \"%s\".",
                teamName,
                oldCategory == null || oldCategory.isBlank() ? "Chưa có" : oldCategory,
                category,
                eventName
        );

        createNotificationNoResponse(
                teamLeaderAccount,
                actor,
                NotificationType.ASSIGNED_CATEGORY,
                NotificationChannel.WEB,
                title,
                message
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
    public void checkResponseNoti(Long notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new BadRequestException("Notification không tồn tại"));

        if (LocalDateTime.now().isAfter(notification.getResponseDeadline())) {
            throw new BadRequestException(
                    "Đã hết thời gian phản hồi");
        }
        if (!notification.isAllowResponse()) {
            throw new BadRequestException(
                    "Thông báo này không cho phép phản hồi");
        }
    }

    @Override
    public List<NotificationWebResponse> getNotifications(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository
                .findByAccount_AccountIdOrderByCreatedAtDesc(accountId)
                .stream().map(this::toResponse).toList();
    }


    @Override
    public List<NotificationWebResponse> getUnreadNotifications(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository
                .findByAccount_AccountIdAndIsReadFalseOrderByCreatedAtDesc(accountId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<NotificationWebResponse> getReadNotifications(CustomUserDetails userDetails) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository
                .findByAccount_AccountIdAndIsReadTrueOrderByCreatedAtDesc(accountId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<NotificationWebResponse> getByType(CustomUserDetails userDetails, NotificationType type) {
        Integer accountId = userDetails.getAccount().getAccountId();
        return notificationRepository
                .findByAccount_AccountIdAndTypeOrderByCreatedAtDesc(accountId, type)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<ResponseEntry> getPendingResponses(CustomUserDetails userDetails) {
        return notificationRepository
                .findNotificationByAccount_AccountIdAndResponseStatus(
                        userDetails.getAccount().getAccountId(),
                        NotiResponseStatus.PENDING)
                .stream().map(this::mapToNotiResponse).toList();
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        // check ownership
        if (notification.getAccount().getAccountId() != accountId) {
            throw new RuntimeException("Bạn không có quyền đọc tất cả thông báo này");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));

        if (notification.getAccount().getAccountId() != accountId) {
            throw new RuntimeException("Bạn không có quyền xóa thông báo này");
        }
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void notifyRoundRankingPublished(Account actor, Integer roundId, boolean isFinal, Integer responseDeadline) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi."));
        String eventName = round.getHackathonEvent().getEventName();
        String title = isFinal ? "Kết quả CHÍNH THỨC: " + round.getRoundName()
                : "Kết quả TẠM THỜI: " + round.getRoundName();
        String noteMessage = isFinal
                ? " Kết quả trên là kết quả chung cuộc chính thức của vòng đấu."
                : " Cổng phúc khảo hiện đã mở. Nếu có khiếu nại về điểm số, vui lòng nộp đơn trên hệ thống trước khi cổng đóng.";
        String message = String.format(
                """
                        Đã có kết quả xếp hạng cho vòng thi "%s" của cuộc thi "%s" 
                        Ban tổ chức đã cập nhật kết quả cuộc thi trên hệ thống WEB FPT HACKATHON.
                        %s
                        Vui lòng kiểm tra chi tiết bảng xếp hạng tại mục kết quả của cuộc thi.
                        Ban tổ chức xin trân trọng và cảm ơn.!
                        
                        """,
                round.getRoundName(),
                eventName,
                noteMessage
        );
        // Gửi thông báo đến all thí sinh thuộc round đó

        List<Account> accounts;
        if (isFinal) {
            List<AccountRole> allRoles = List.of(
                    AccountRole.STUDENT,
                    AccountRole.EVENTCOORDINATOR,
                    AccountRole.EXPERT);
            accounts = accountRepository.findByRoleIn(allRoles);
        } else {

            accounts = accountRepository.findParticipantsByRoundId(roundId);
        }
        for (Account acc : accounts) {
            if (isFinal) {
                createNotificationNoResponse(
                        acc,
                        actor,
                        NotificationType.RANKING_OFFICIAL,
                        NotificationChannel.WEB,
                        title,
                        message
                );
            } else {
                createNotificationHaveResponse(
                        acc,
                        actor,
                        NotificationType.RANKING_DRAFT,
                        NotificationChannel.WEB,
                        title,
                        message,
                        true,
                        responseDeadline
                );
            }
            try {
                emailService.sendGeneralEmail(acc.getEmail(), title, message);
                createNotificationNoResponse(
                        acc,
                        actor,
                        isFinal ? NotificationType.RANKING_OFFICIAL : NotificationType.RANKING_DRAFT,
                        NotificationChannel.EMAIL,
                        title,
                        message
                );
            } catch (Exception e) {
                System.out.println("Lỗi gửi email cho thí sinh xem hạng");
            }

        }
    }

    @Override
    public void notifyExpertReEvaluation(Account actor, Set<Account> expertsToNotify, String teamName) {
        String title = "YÊU CẦU PHÚC KHẢO BÀI THI";
        String message = "Ban tổ chức yêu cầu ban giám khảo xem lại và chấm lại điểm số cho bài dự thi của đội " + teamName+
                "Ban tổ chức xin trân trọng và cảm ơn.!";
        for (Account expertAccount : expertsToNotify) {
            createNotificationNoResponse(
                    expertAccount,
                    actor,
                    NotificationType.SUBMISSION_REVIEW,
                    NotificationChannel.WEB,
                    title,
                    message
            );

            try {
                emailService.sendNotifyToExpertReEvaluation(expertAccount.getEmail(), message);
                createNotificationNoResponse(
                        expertAccount,
                        actor,
                        NotificationType.SUBMISSION_REVIEW,
                        NotificationChannel.EMAIL,
                        title,
                        message
                );
            } catch (Exception e) {
                System.out.println("Lỗi gửi email cho giám khảo để yêu cầu giám khảo chấm lại bài nộp của thí sinh");
            }
        }

    }

    @Override
    public void notifyResponseAppeal(Account actor, Account account , String teamName, boolean isChanged) {
        String statusContent = isChanged
                ? "chấp nhận và đã cập nhật lại điểm số cho"
                : "xem xét và quyết định giữ nguyên kết quả hiện tại của";
        String title = " KẾT QUẢ YÊU CẦU PHÚC KHẢO BÀI THI";
        String message = String.format(
                """
                THÔNG BÁO KẾT QUẢ PHÚC KHẢO:

                Chào đội thi "%s",     
                Ban tổ chức đã "%s" đơn phúc khảo của các bạn.
                                     
                Bạn vui lòng kiểm tra lại điểm số chi tiết tại Dashboard trên hệ thống WEB FPT HACKATHON.
                Trân trọng,
                Ban Tổ Chức.
                """,
                teamName,
                statusContent
        );
        try {
            emailService.sendGeneralEmail(account.getEmail(), title,message);
            createNotificationNoResponse(
                    account,
                    actor,
                    NotificationType.RESULT_APPEAL,
                    NotificationChannel.EMAIL,
                    title,
                    message
            );
            createNotificationNoResponse(
                    account,
                    actor,
                    NotificationType.RESULT_APPEAL,
                    NotificationChannel.WEB,
                    title,
                    message
            );
        } catch (Exception e) {
            System.out.println("Lỗi gửi email cho giám khảo để yêu cầu giám khảo chấm lại bài nộp của thí sinh");
        }

    }

    @Override
    public void notifyMentorSupportTeam(Account actor, List<ExpertAssign> mentors) {
        String title ="THÔNG BÁO YÊU CẦU HỖ TRỢ TEAM";
        String message = "Ban tổ chức xin thông báo đến các Mentor đang tham gia hỗ trợ cuộc thi. "
                + "Hiện tại có yêu cầu hỗ trợ mới từ đội thi, vui lòng kiểm tra và phản hồi trong thời gian sớm nhất. "
                + "Ban tổ chức xin trân trọng cảm ơn.!";
        for (ExpertAssign assign : mentors) {

            Account mentorAccount = assign.getExpert().getAccount();

            createNotificationNoResponse(
                    mentorAccount,
                    actor,
                    NotificationType.SUPPORT_TEAM,
                    NotificationChannel.WEB,
                    title,
                    message
            );
        }
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

    private ResponseEntry mapToNotiResponse(Notification notification) {
        return ResponseEntry.builder().senderId(notification.getActor().getAccountId()).senderName(notification.getActor().getStudent().getStudentName()).message(notification.getResponseMessage()).build();
    }


}
