package com.hackathon.validator;

import com.hackathon.dto.team.ProcessTeamRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.Registration;
import com.hackathon.entity.Round;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.TeamRequest;
import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.NotificationType;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.entity.enums.RequestType;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.TeamRequestRepository;
import com.hackathon.repository.AccountRepository;
import com.hackathon.repository.TeamRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TeamRequestValidator {

    private final TeamRequestRepository teamRequestRepository;
    private final AccountRepository accountRepository;
    private final TeamRepository teamRepository;

    public Account requireStudentAccount(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getAccount() == null) {
            throw new BadRequestException("Không tìm thấy thông tin tài khoản");
        }

        return accountRepository.findById(userDetails.getAccount().getAccountId())
                .filter(account -> account.getStudent() != null)
                .orElseThrow(() -> new BadRequestException(
                        "Chỉ tài khoản sinh viên mới có thể tạo yêu cầu"));
    }

    public Team requireActiveLeadingTeam(Account account) {
        return teamRepository.findActiveLeadingTeamByStudentId(
                        account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải leader của team đang tham gia"));
    }

    public TeamParticipant requireParticipantInRound(Team team, Round round) {
        return team.getRegistrations().stream()
                .filter(registration -> registration.getStatus()
                        == com.hackathon.entity.enums.RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(participant -> participant.getCategoryRound() != null
                        && participant.getCategoryRound().getRound().getRoundId()
                        .equals(round.getRoundId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Team không tham gia vòng thi này"));
    }

    public void validateNotificationResponse(
            Notification notification,
            Account account
    ) {
        if (notification.getAccount() == null
                || notification.getAccount().getAccountId() != account.getAccountId()) {
            throw new BadRequestException(
                    "Bạn không có quyền phản hồi thông báo này");
        }
        if (!notification.isAllowResponse()) {
            throw new BadRequestException(
                    "Thông báo này không cho phép phản hồi");
        }

        LocalDateTime now = LocalDateTime.now();
        if (notification.getType() == NotificationType.ASSIGNED_CATEGORY) {
            if (notification.getRound() == null) {
                throw new BadRequestException(
                        "Thông báo kết quả bốc thăm chưa liên kết với vòng thi");
            }
            if (notification.getRound().getHackathonEvent() == null) {
                throw new BadRequestException(
                        "Không tìm thấy sự kiện của kết quả bốc thăm");
            }
            if (notification.getRound().getHackathonEvent().getStartDate() != null
                    && !now.isBefore(notification.getRound().getHackathonEvent().getStartDate())) {
                throw new BadRequestException(
                        "Sự kiện đã bắt đầu, không thể gửi yêu cầu xác minh kết quả bốc thăm");
            }
        }

        if (notification.getResponseDeadline() == null
                || now.isAfter(notification.getResponseDeadline())) {
            throw new BadRequestException("Đã hết thời hạn phản hồi");
        }
        if (notification.getResponseStatus() != null
                && notification.getResponseStatus() != NotiResponseStatus.NONE) {
            throw new BadRequestException(
                    "Thông báo này đã được phản hồi trước đó");
        }
    }

    public void validateNoOpenRequest(
            Team team,
            Round round,
            RequestType requestType
    ) {
        if (round == null) {
            throw new BadRequestException(
                    "Thông báo chưa liên kết với vòng thi");
        }

        boolean exists = teamRequestRepository
                .existsByTeam_TeamIdAndRound_RoundIdAndStatusInAndRequestType(
                        team.getTeamId(),
                        round.getRoundId(),
                        List.of(
                                RequestStatus.PENDING,
                                RequestStatus.IN_REVIEW,
                                RequestStatus.PROCESSING
                        ),
                        requestType
                );
        if (exists) {
            throw new BadRequestException(
                    "Team đã có một yêu cầu cùng loại đang được xử lý");
        }
    }

//    public void validateDrawResultUpdate(
//            TeamRequest teamRequest,
//            ProcessTeamRequest command
//    ) {
//        if (command.getEventId() == null) {
//            throw new BadRequestException(
//                    "Event id không được để trống");
//        }
//        if (command.getDrawResults() == null
//                || command.getDrawResults().isEmpty()) {
//            throw new BadRequestException(
//                    "Kết quả bốc thăm cập nhật không được để trống");
//        }
//
//        Set<Integer> registrationIds = teamRequest.getTeam().getRegistrations()
//                .stream()
//                .filter(registration -> registration.getHackathonEvent() != null
//                        && registration.getHackathonEvent().getEventId()
//                        == command.getEventId())
//                .map(Registration::getRegistrationId)
//                .collect(Collectors.toSet());
//
//        if (registrationIds.isEmpty()) {
//            throw new BadRequestException(
//                    "Team không đăng ký tham gia event này");
//        }
//
//        boolean invalid = command.getDrawResults().stream()
//                .anyMatch(result -> result.getRegistrationId() == null
//                        || result.getRegistrationId().isEmpty()
//                        || result.getRegistrationId().stream()
//                        .anyMatch(id -> !registrationIds.contains(id)));
//
//        if (invalid) {
//            throw new BadRequestException(
//                    "Chỉ được cập nhật kết quả bốc thăm của team đã gửi yêu cầu");
//        }
//    }
}
