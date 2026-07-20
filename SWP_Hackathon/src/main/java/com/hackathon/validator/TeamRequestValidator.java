package com.hackathon.validator;

import com.hackathon.dto.team.ProcessTeamRequest;
import com.hackathon.entity.Account;
import com.hackathon.entity.Notification;
import com.hackathon.entity.Registration;
import com.hackathon.entity.Round;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamRequest;
import com.hackathon.entity.enums.NotiResponseStatus;
import com.hackathon.entity.enums.RequestStatus;
import com.hackathon.entity.enums.RequestType;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.TeamRequestRepository;
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
        if (notification.getResponseDeadline() == null
                || LocalDateTime.now().isAfter(notification.getResponseDeadline())) {
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
