package com.hackathon.service.teamRequest;

import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamRequestServiceImpl implements TeamRequestService {
    private final TeamRepository teamRepository;
    private final ExpertRepository expertRepository;
    private final HackathonEventRepository hackathonEventRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final TeamRequestRepository teamRequestRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final StudentRepository studentRepository;
    private final RoundRepository roundRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final NotificationRepository notificationRepository;

    private TeamRequestResponse mapToResponse(TeamRequest rq, CategoryRound cr, Integer expertId) {
        return TeamRequestResponse.builder()
                .requestId(rq.getRequestId())
                .teamId(rq.getTeam().getTeamId())
                .teamName(rq.getTeam().getTeamName())
                .expertId(expertId)
                .createDate(rq.getCreateDate())
                .status(rq.getStatus())
                .responseAt(rq.getResponseAt())
                .round(cr != null ? cr.getRound().getRoundName() : (rq.getRound() != null ? rq.getRound().getRoundName() : "N/A"))
                .categoryName(cr != null ? cr.getCategory().getCategoryName() : "N/A")
                .requestMessage(rq.getRequestMessage())
                .responseMessage(rq.getResponseMessage())
                .responseStatus(rq.getResponseStatus())
                .build();
    }


    @Override
    @Transactional
    public List<TeamRequestResponse> teamSendRequestToMentor(String requestMessage, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException("Tài khoản này không phải là tài khoản student");
        }
        //Tìm Team mà Student này làm leader và đang trạng thái thi đấu
        Team team = teamRepository.findActiveLeadingTeamByStudentId(account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải leader của đội đang tham gia thi đấu"));

        // Kiểm tra xem Đội này đã có yêu cầu nào đang chờ (PENDING)  chưa
        // Nếu có ko dc gửi nx , tránh spam nhiều lần
        boolean hasPendingRequest = teamRequestRepository.existsByTeam_TeamIdAndStatus(team.getTeamId(), RequestStatus.PENDING);
        if (hasPendingRequest) {
            throw new BadRequestException("Đội của bạn đã có một yêu cầu đang nằm trong danh sách chờ. Vui lòng đợi Mentor xử lý trước khi gửi yêu cầu mới!");
        }
        TeamParticipant activeParticipant = team.getRegistrations().stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(p -> p != null
                        && p.getCategoryRound() != null
                        && p.getCategoryRound().getRound().getStatus() == RoundStatus.ONGOING)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Hiện tại không có vòng thi nào đang diễn ra hoặc đội chưa có suất tham gia hợp lệ."));
        CategoryRound categoryRound = activeParticipant.getCategoryRound();

        List<ExpertAssign> expertAssign = expertAssignRepository.findByCategoryRoundId(categoryRound.getCategoryRoundId());
        if (expertAssign == null || expertAssign.isEmpty()) {
            throw new BadRequestException("Đội của bạn hiện tại chưa được Ban tổ chức phân công Mentor phụ trách ở vòng này.");
        }

        TeamRequest newRequest = new TeamRequest();
        newRequest.setTeam(team);
        newRequest.setExpertAssign(null);
        newRequest.setCreateDate(LocalDateTime.now());
        newRequest.setStatus(RequestStatus.PENDING);
        newRequest.setRequestMessage(requestMessage);
        newRequest.setResponseStatus(NotiResponseStatus.PENDING);
        newRequest.setRequestType(RequestType.MENTOR_SUPPORT);
        TeamRequest saveTeam = teamRequestRepository.save(newRequest);

        return List.of(mapToResponse(saveTeam, categoryRound, null));

    }

    @Override
    public List<TeamRequestResponse> getTeamRequestsForExpert(CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin Expert tương ứng với tài khoản này."));
        // Lấy ds các Team gửi request
        List<TeamRequest> listRequest = teamRequestRepository.findRequestForExpertRoleMentor(expert.getExpertId());
        if (listRequest == null || listRequest.isEmpty()) {
            return new ArrayList<>();
        }

        List<TeamRequestResponse> responseList = new ArrayList<>();
        for (TeamRequest rq : listRequest) {
            CategoryRound categoryRound = rq.getTeam().getRegistrations().stream()
                    .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED)
                    .map(Registration::getParticipants)
                    .flatMap(List::stream)
                    .filter(p -> p != null && p.getCategoryRound() != null && p.getCategoryRound().getRound().getStatus() == RoundStatus.ONGOING)
                    .map(TeamParticipant::getCategoryRound)
                    .findFirst()
                    .orElse(null);

            TeamRequestResponse response = TeamRequestResponse.builder()
                    .requestId(rq.getRequestId())
                    .teamId(rq.getTeam().getTeamId())
                    .teamName(rq.getTeam().getTeamName())
                    .expertId(expert.getExpertId())
                    .createDate(rq.getCreateDate())
                    .status(rq.getStatus())
                    .round(categoryRound != null ? categoryRound.getRound().getRoundName() : "N/A")
                    .categoryName(categoryRound != null ? categoryRound.getCategory().getCategoryName() : "N/A")
                    .responseStatus(rq.getResponseStatus())
                    .requestMessage(rq.getRequestMessage())
                    .build();
            responseList.add(response);
        }


        return responseList;
    }

    //-------------------------------------//
    // MENTOR: CHẤP NHẬN VÀ TỪ CHỐI YÊU CẦU TỪ ĐỘI THI
    //-------------------------------------//

    @Override
    @Transactional
    public TeamRequestResponse acceptTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Expert vì vậy không được phép truy cập vào trình duyệt này."));
        // Check expert có quản lý Team được gửi yêu cầu không
        TeamRequest teamRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy yêu cầu này"));
        if (teamRequest.getStatus() != RequestStatus.PENDING || teamRequest.getExpertAssign() != null) {
            throw new BadRequestException("Yêu cầu này vừa mới được một Mentor khác nhanh tay tiếp nhận hỗ trợ mất rồi!");
        }

        // Tìm hạng mục mà Mentor này đang được phân công
        CategoryRound categoryRound = teamRequest.getTeam().getRegistrations().stream()
                .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(p -> p != null
                        && p.getCategoryRound() != null
                        && p.getCategoryRound().getRound().getStatus() == RoundStatus.ONGOING)
                .map(TeamParticipant::getCategoryRound)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không xác định được vòng thi hợp lệ cho đội này."));

        ExpertAssign mySpecificAssign = expertAssignRepository
                .findMentorByExpertIdAndCategoryRoundId(categoryRound.getCategoryRoundId(), expert.getExpertId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Mentor phụ trách đội thi này ở vòng đấu hiện tại."));

        teamRequest.setStatus(RequestStatus.ACCEPTED);
        teamRequest.setResponseStatus(NotiResponseStatus.NONE);
        teamRequest.setResponseAt(LocalDateTime.now());
        teamRequest.setExpertAssign(mySpecificAssign);
        teamRequest.setResponder(account);
        if (responseMessage != null) {
            teamRequest.setResponseMessage(responseMessage);
        } else {
            teamRequest.setResponseMessage("Yêu cầu đã được chấp nhận.");

        }
        try {
            TeamRequest updateRequest = teamRequestRepository.save(teamRequest);

            return mapToResponse(updateRequest, categoryRound, expert.getExpertId());

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Yêu cầu này vừa mới được một Mentor khác tiếp nhận hỗ trợ mất rồi!");
        }

    }


    @Override
    @Transactional
    public TeamRequestResponse rejectTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Expert vì vậy không được phép truy cập vào trình duyệt này."));
        // Check expert có quản lý Team được gửi yêu cầu không
        TeamRequest teamRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy yêu cầu này"));

        if (teamRequest.getStatus() != RequestStatus.PENDING || teamRequest.getExpertAssign() != null) {
            throw new BadRequestException("Yêu cầu này vừa mới được một Mentor khác nhanh tay tiếp nhận hỗ trợ mất rồi!");
        }

        // Tìm hạng mục mà Mentor này đang được phân công
        CategoryRound categoryRound = teamRequest.getTeam().getRegistrations().stream()
                .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(p -> p != null
                        && p.getCategoryRound() != null
                        && p.getCategoryRound().getRound().getStatus() == RoundStatus.ONGOING)
                .map(TeamParticipant::getCategoryRound)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không xác định được vòng thi hợp lệ cho đội này."));

        ExpertAssign mySpecificAssign = expertAssignRepository
                .findMentorByExpertIdAndCategoryRoundId(categoryRound.getCategoryRoundId(), expert.getExpertId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Mentor phụ trách đội thi này ở vòng đấu hiện tại."));

        teamRequest.setExpertAssign(mySpecificAssign);
        teamRequest.setStatus(RequestStatus.DECLINED);
        teamRequest.setResponseStatus(NotiResponseStatus.NONE);
        teamRequest.setResponseAt(LocalDateTime.now());
        teamRequest.setResponder(account);
        if (responseMessage == null || responseMessage.isEmpty()) {
            teamRequest.setResponseMessage("Yêu cầu đã được từ chối");
        } else {
            teamRequest.setResponseMessage(responseMessage);
        }
        try {
            TeamRequest updateRequest = teamRequestRepository.save(teamRequest);

            return mapToResponse(updateRequest, categoryRound, expert.getExpertId());

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Yêu cầu này vừa mới được một Mentor khác xử lý mất rồi!");
        }


    }

    @Override
    public List<TeamRequestResponse> teamSendAppealRequest(TeamAppealRequestDTO request, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();

        Student student = studentRepository.findById(account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản sinh viên"));

        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        LocalDateTime now = LocalDateTime.now();
        if (round.getStatus() != RoundStatus.PUBLIC_DRAFT) {
            throw new BadRequestException("Vòng đấu hiện không nằm trong giai đoạn tiếp nhận khiếu nại.");
        }
        if (round.getAppealStartTime() == null || round.getAppealEndTime() == null) {
            throw new BadRequestException("Cổng khiếu nại của vòng đấu này chưa được thiết lập thời gian.");
        }
        if (now.isBefore(round.getAppealStartTime())) {
            throw new BadRequestException("Cổng khiếu nại chưa đến giờ mở. Vui lòng quay lại lúc: " + round.getAppealStartTime());
        }
        if (now.isAfter(round.getAppealEndTime())) {
            throw new BadRequestException("Cổng tiếp nhận khiếu nại đã chính thức đóng lại.");
        }

        TeamMember teamMember = student.getTeamMembers().stream()
                .filter(tm -> tm.getTeam().getRegistrations()
                        .stream().anyMatch(registration ->
                                registration.getHackathonEvent().getEventId() == round.getHackathonEvent().getEventId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không thuộc đội thi của sự kiện này."));
        if (!teamMember.getIsLeader()) {
            throw new BadRequestException("Bạn không phải leader, vì vậy bạn không có quyền khiếu nại.");
        }

        boolean hasPendingRequest = teamRequestRepository.existsByTeam_TeamIdAndStatus(teamMember.getTeam().getTeamId(), RequestStatus.PENDING);
        if (hasPendingRequest) {
            throw new BadRequestException("Đội của bạn đã gửi một đơn khiếu nại trước đó và đang chờ xử lý.");
        }
        // Lưu đơn khiếu nại
        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setRequestMessage(request.getRequestMessage());
        teamRequest.setRequestId(teamRequest.getRequestId());
        teamRequest.setTeam(teamMember.getTeam());
        teamRequest.setRequestType(RequestType.APPEAL);
        teamRequest.setCreateDate(LocalDateTime.now());
        teamRequest.setStatus(RequestStatus.PENDING);
        teamRequest.setResponder(account);
        teamRequest.setRound(round);
        teamRequest.setResponseStatus(NotiResponseStatus.PENDING);
        teamRequest.setResponseMessage(null);

        TeamRequest saveTeam = teamRequestRepository.save(teamRequest);

        return List.of(mapToResponse(saveTeam, null, null));
    }

    @Override
    public List<TeamRequestResponse> getAppealRequest(CustomUserDetails userDetails, Integer roundId) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));

        // Lấy ds đơn khiếu nại
        List<TeamRequest> appealRequest = teamRequestRepository.findByRound_RoundIdAndRequestType(roundId, RequestType.APPEAL);
        if (appealRequest == null || appealRequest.isEmpty()) {
            throw new BadRequestException("Không tìm thấy đơn khiếu nại của round id: " + roundId);
        }
        List<TeamRequestResponse> responseList = new ArrayList<>();
        for (TeamRequest rq : appealRequest) {
            TeamRequestResponse response = TeamRequestResponse.builder()
                    .requestId(rq.getRequestId())
                    .teamId(rq.getTeam().getTeamId())
                    .teamName(rq.getTeam().getTeamName())
                    .createDate(rq.getCreateDate())
                    .status(rq.getStatus())
                    .round(rq.getRound().getRoundName())
                    .requestMessage(rq.getRequestMessage())
                    .responseMessage(rq.getResponseMessage())
                    .responseStatus(rq.getResponseStatus())
                    .responseAt(rq.getResponseAt())
                    .build();
            responseList.add(response);

        }
        return responseList;
    }

    @Override
    public TeamRequestResponse rejectAppealRequest(CustomUserDetails userDetails, Integer requestId, String responseMessage) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        TeamRequest appealRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại với id: " + requestId));
        if (appealRequest.getRequestType() != RequestType.APPEAL) {
            throw new BadRequestException("Đây không phải là đơn khiếu nại kết quả.");
        }
        if (appealRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Đơn khiếu nại này đã được ban tổ chức xử lý trước đó.");
        }

        appealRequest.setResponseStatus(NotiResponseStatus.NONE);
        appealRequest.setStatus(RequestStatus.DECLINED);
        appealRequest.setResponseMessage(responseMessage != null ? responseMessage : "BTC đã xử lý đơn khiếu nại.");
        appealRequest.setResponder(account);
        appealRequest.setResponseAt(LocalDateTime.now());
        TeamRequest updated = teamRequestRepository.save(appealRequest);
        return mapToResponse(updated, null, null);
    }

    @Override
    public TeamRequestResponse acceptAppealRequest(CustomUserDetails userDetails, Integer requestId, String responseMessage) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        TeamRequest appealRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại với id: " + requestId));
        if (appealRequest.getRequestType() != RequestType.APPEAL) {
            throw new BadRequestException("Đây không phải là đơn khiếu nại kết quả.");
        }

        if (appealRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Đơn khiếu nại này đã được ban tổ chức xử lý trước đó.");
        }
        appealRequest.setResponseStatus(NotiResponseStatus.NONE);
        appealRequest.setStatus(RequestStatus.ACCEPTED);
        appealRequest.setResponseMessage(responseMessage != null ? responseMessage : "BTC đã xử lý đơn khiếu nại.");
        appealRequest.setResponder(account);
        appealRequest.setResponseAt(LocalDateTime.now());
        TeamRequest updated = teamRequestRepository.save(appealRequest);
        return mapToResponse(updated, null, null);
    }

    @Override
    public TeamRequestResponse requestExpertToReEvaluation(CustomUserDetails userDetails, Integer requestId) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        TeamRequest appealRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại với id: " + requestId));
        if (appealRequest.getRequestType() != RequestType.APPEAL) {
            throw new BadRequestException("Đây không phải là đơn khiếu nại kết quả.");
        }

        if (appealRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Đơn khiếu nại này đã được ban tổ chức xử lý trước đó.");
        }

        // Tim expert phu trách bài nộp đó để xem và đánh giá lại
        CategoryRound categoryRound = appealRequest.getTeam().getRegistrations().stream()
                .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(p -> p != null && p.getCategoryRound() != null
                        && p.getCategoryRound().getRound().getRoundId().equals(appealRequest.getRound().getRoundId()))
                .map(TeamParticipant::getCategoryRound)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy hạng mục thi đấu hợp lệ của đội thi tại vòng này."));

        List<ExpertAssign> judges = expertAssignRepository
                .findByCategoryRoundId(categoryRound.getCategoryRoundId())
                .stream()
                .filter(assign ->
                        assign.getRole() == ExpertRole.CORE_JUDGE
                                || assign.getRole() == ExpertRole.GUEST_JUDGE)
                .toList();
        if (judges.isEmpty()) {
            throw new BadRequestException("Không tìm thấy Expert phụ trách bài thi này.");
        }
        for (ExpertAssign ex : judges) {
            Notification notification = new Notification();
            notification.setChannel(NotificationChannel.WEB);
            notification.setMessage("Ban tổ chức yêu cầu bạn xem xét lại kết qur của Team: " +
                    appealRequest.getTeam().getTeamName());
            notification.setType(NotificationType.SUBMISSION_REVIEW);
            notification.setAccount(ex.getExpert().getAccount());
            notificationRepository.save(notification);

        }
        appealRequest.setStatus(RequestStatus.IN_REVIEW);
        teamRequestRepository.save(appealRequest);
        TeamRequest updated = teamRequestRepository.save(appealRequest);

        return mapToResponse(updated, null, null);
    }


}

