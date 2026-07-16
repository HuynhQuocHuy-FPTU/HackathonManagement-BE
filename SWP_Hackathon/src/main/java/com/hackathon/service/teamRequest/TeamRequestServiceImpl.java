package com.hackathon.service.teamRequest;

import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.evaluation.EvaluationDetailResponse;
import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.dto.submission.FileDTO;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.dto.team.ProcessTeamRequest;
import com.hackathon.dto.team.CreateDirectTeamRequest;
import com.hackathon.dto.notification.NotiResponseRequest;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AuditService;
import com.hackathon.service.LuckyDrawResultService;
import com.hackathon.service.NotificationService;
import com.hackathon.validator.TeamRequestValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j

@Service
@RequiredArgsConstructor
public class TeamRequestServiceImpl implements TeamRequestService {
    private final TeamRepository teamRepository;
    private final ExpertRepository expertRepository;

    private final ExpertAssignRepository expertAssignRepository;
    private final TeamRequestRepository teamRequestRepository;
    private final StudentRepository studentRepository;
    private final RoundRepository roundRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EvaluationRepository evaluationRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final LuckyDrawResultService luckyDrawResultService;
    private final ParticipantRepository participantRepository;
    private final TeamRequestValidator teamRequestValidator;

    @Override
    @Transactional
    public TeamRequestResponse respondNotification(
            CustomUserDetails userDetails,
            Long notificationId,
            NotiResponseRequest command
    ) {
        Account account = userDetails.getAccount();
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException(
                    "Chỉ tài khoản student mới có thể phản hồi notification");
        }

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy notification"));

        teamRequestValidator.validateNotificationResponse(
                notification, account);

        TeamRequest teamRequest = switch (notification.getType()) {
            case ASSIGNED_CATEGORY ->
                    buildDrawVerificationRequest(
                            notification, account, command.getMessage());
            case SUBMISSION_SCORED ->
                    buildAppealRequest(notification, account, command);
            default -> throw new BadRequestException(
                    "Notification này không hỗ trợ tạo TeamRequest");
        };

        teamRequest.setSourceNotification(notification);
        TeamRequest savedRequest = teamRequestRepository.save(teamRequest);

        notification.setResponseStatus(NotiResponseStatus.APPROVED);
        notification.setResponseMessage(command.getMessage());
        notification.setResponseAt(LocalDateTime.now());
        notification.setAllowResponse(false);
        notificationRepository.save(notification);

        return toResponse(savedRequest, null, null);
    }

    private TeamRequest buildDrawVerificationRequest(
            Notification notification,
            Account account,
            String requestMessage
    ) {
        Team team = account.getStudent().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getTeam)
                .filter(item -> notification.getTeam() != null
                        && item.getTeamId() == notification.getTeam().getTeamId())
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải leader của team nhận kết quả bốc thăm"));

        teamRequestValidator.validateNoOpenRequest(
                team, notification.getRound(),
                RequestType.DRAW_RESULT_VERIFICATION);

        boolean hasAssignedCategory = team.getRegistrations().stream()
                .filter(registration ->
                        registration.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .anyMatch(participant -> participant.getCategoryRound() != null);
        if (!hasAssignedCategory) {
            throw new BadRequestException(
                    "Team chưa có kết quả bốc thăm để xác thực");
        }

        return newPendingRequest(
                team,
                notification.getRound(),
                RequestType.DRAW_RESULT_VERIFICATION,
                requestMessage
        );
    }

    private TeamRequest buildAppealRequest(
            Notification notification,
            Account account,
            NotiResponseRequest command
    ) {
        if (command.getRoundId() == null) {
            throw new BadRequestException(
                    "Round id không được để trống khi khiếu nại điểm");
        }

        Round round = roundRepository.findById(command.getRoundId())
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy vòng thi"));

        if (notification.getRound() != null
                && !notification.getRound().getRoundId().equals(round.getRoundId())) {
            throw new BadRequestException(
                    "Notification điểm không thuộc vòng thi này");
        }

        Team team = account.getStudent().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getTeam)
                .filter(item -> item.getRegistrations().stream()
                        .filter(registration ->
                                registration.getStatus() == RegistrationStatus.APPROVED)
                        .map(Registration::getParticipants)
                        .flatMap(List::stream)
                        .anyMatch(participant ->
                                participant.getCategoryRound() != null
                                && participant.getCategoryRound().getRound()
                                .getRoundId().equals(round.getRoundId())))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải leader của team tham gia vòng thi này"));

        teamRequestValidator.validateNoOpenRequest(
                team, round, RequestType.APPEAL);

        return newPendingRequest(
                team, round, RequestType.APPEAL, command.getMessage());
    }

    private TeamRequest newPendingRequest(
            Team team,
            Round round,
            RequestType requestType,
            String requestMessage
    ) {
        TeamRequest request = new TeamRequest();
        request.setTeam(team);
        request.setRound(round);
        request.setRequestType(requestType);
        request.setRequestMessage(requestMessage);
        request.setCreateDate(LocalDateTime.now());
        request.setStatus(RequestStatus.PENDING);
        return request;
    }


    //---------------------------------------------//
    // TEAM YÊU CẦU SỰ HỔ TRỢ TỪ MENTOR
    //---------------------------------------------//
    @Override
    @Transactional
    public List<TeamRequestResponse> teamSendRequestToMentor(TeamAppealRequestDTO request, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException("Tài khoản này không phải là tài khoản student");
        }
        //Tìm Team mà Student này làm leader và đang trạng thái thi đấu
        Team team = teamRepository.findActiveLeadingTeamByStudentId(account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải leader của đội đang tham gia thi đấu"));

        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        // Kiểm tra xem Đội này đã có yêu cầu nào đang chờ (PENDING)  chưa
        // Nếu có ko dc gửi nx , tránh spam nhiều lần
        boolean hasPendingRequest = teamRequestRepository.existsByTeam_TeamIdAndStatusAndRequestType(team.getTeamId(), RequestStatus.PENDING, RequestType.MENTOR_SUPPORT);
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
        newRequest.setRequestMessage(request.getRequestMessage());
        newRequest.setRound(round);
//        newRequest.setResponseStatus(NotiResponseStatus.PENDING);
        newRequest.setRequestType(RequestType.MENTOR_SUPPORT);
        TeamRequest saveTeam = teamRequestRepository.save(newRequest);

        auditService.saveLog(
                account,
                AuditAction.SEND_MENTOR_REQUEST,
                AuditEntityType.TEAM,
                team.getTeamId(),
                "Team gửi yêu cầu đến Mentor hỗ trợ thành công"
        );
        return List.of(toResponse(
                saveTeam, categoryRound, null));


    }

    @Override
    public Page<TeamRequestResponse> getTeamRequestsForExpert(
            CustomUserDetails userDetails,
            Pageable pageable
    ) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin Expert tương ứng với tài khoản này."));
        // Lấy ds các Team gửi request
        Page<TeamRequest> requestPage =
                teamRequestRepository.findRequestForExpertRoleMentor(
                        expert.getExpertId(), pageable);

        return requestPage.map(rq -> {
            CategoryRound categoryRound = rq.getTeam().getRegistrations().stream()
                    .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED)
                    .map(Registration::getParticipants)
                    .flatMap(List::stream)
                    .filter(p -> p != null && p.getCategoryRound() != null && p.getCategoryRound().getRound().getStatus() == RoundStatus.ONGOING)
                    .map(TeamParticipant::getCategoryRound)
                    .findFirst()
                    .orElse(null);

            return TeamRequestResponse.builder()
                    .requestId(rq.getRequestId())
                    .teamId(rq.getTeam().getTeamId())
                    .teamName(rq.getTeam().getTeamName())
                    .expertId(expert.getExpertId())
                    .createDate(rq.getCreateDate())
                    .status(rq.getStatus())
                    .round(categoryRound != null ? categoryRound.getRound().getRoundName() : "N/A")
                    .categoryName(categoryRound != null ? categoryRound.getCategory().getCategoryName() : "N/A")
                    .requestMessage(rq.getRequestMessage())
                    .build();
        });
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

        teamRequest.setStatus(RequestStatus.IN_REVIEW);
        teamRequest.setResponseAt(LocalDateTime.now());
        teamRequest.setExpertAssign(mySpecificAssign);
        teamRequest.setResponder(account);

        auditService.saveLog(
                account,
                AuditAction.MENTOR_ACCEPT_REQUEST,
                AuditEntityType.TEAM,
                teamRequest.getTeam().getTeamId(),
                "Chấp nhận yêu câud hỗ trợ từ team thành công"
        );

        if (responseMessage != null) {
            teamRequest.setResponseMessage(responseMessage);
        } else {
            teamRequest.setResponseMessage("Yêu cầu đã được chấp nhận.");

        }
        try {
            TeamRequest updateRequest = teamRequestRepository.save(teamRequest);

            return toResponse(
                    updateRequest, categoryRound, expert.getExpertId());

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
        teamRequest.setStatus(RequestStatus.REJECTED);
        teamRequest.setResponseAt(LocalDateTime.now());
        teamRequest.setResponder(account);
        if (responseMessage == null || responseMessage.isEmpty()) {
            teamRequest.setResponseMessage("Yêu cầu đã được từ chối");
        } else {
            teamRequest.setResponseMessage(responseMessage);
        }

        auditService.saveLog(
                account,
                AuditAction.MENTOR_REJECT_REQUEST,
                AuditEntityType.TEAM,
                teamRequest.getTeam().getTeamId(),
                "Từ chối yêu cầu hỗ trợ từ team thành công"
        );
        try {
            TeamRequest updateRequest = teamRequestRepository.save(teamRequest);

            return toResponse(
                    updateRequest, categoryRound, expert.getExpertId());

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Yêu cầu này vừa mới được một Mentor khác xử lý mất rồi!");
        }


    }
    @Override
    public Page<TeamRequestResponse> getAppealRequest(
            CustomUserDetails userDetails,
            Integer roundId,
            Pageable pageable
    ) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));

        // Lấy ds đơn khiếu nại
        Page<TeamRequest> appealPage = teamRequestRepository
                .findByRound_RoundIdAndRequestType(
                        roundId, RequestType.APPEAL, pageable);
        if (appealPage.isEmpty()) {
            throw new BadRequestException(
                    "Không tìm thấy đơn khiếu nại của round id: " + roundId);
        }

        return appealPage.map(rq -> TeamRequestResponse.builder()
                    .requestId(rq.getRequestId())
                    .teamId(rq.getTeam().getTeamId())
                    .teamName(rq.getTeam().getTeamName())
                    .createDate(rq.getCreateDate())
                    .status(rq.getStatus())
                    .round(rq.getRound().getRoundName())
                    .requestMessage(rq.getRequestMessage())
                    .responseMessage(rq.getResponseMessage())
                    .responseAt(rq.getResponseAt())
                    .build());
    }

    @Override
    public Page<TeamRequestResponse> getAppealRequestPublic(
            CustomUserDetails userDetails,
            Integer roundId,
            Pageable pageable
    ) {
        Account account = userDetails.getAccount();
        if(account == null) {
            throw new BadRequestException("Account không tồn tại");
        }
        Page<TeamRequest> appealPage =
                teamRequestRepository.findByRound_RoundId(
                        roundId, pageable);
        if (appealPage.isEmpty()) {
            throw new BadRequestException(
                    "Không tìm thấy đơn khiếu nại của round id: " + roundId);
        }

        return appealPage.map(rq -> TeamRequestResponse.builder()
                    .requestId(rq.getRequestId())
                    .teamId(rq.getTeam().getTeamId())
                    .teamName(rq.getTeam().getTeamName())
                    .requestType(rq.getRequestType())
                    .createDate(rq.getCreateDate())
                    .status(rq.getStatus())
                    .round(rq.getRound().getRoundName())
                    .requestMessage(rq.getRequestMessage())
                    .responseMessage(rq.getResponseMessage())
                    .responseAt(rq.getResponseAt())
                    .build());
    }


    private TeamRequestResponse rejectAppealRequest(CustomUserDetails userDetails, Integer requestId, String responseMessage) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        TeamRequest appealRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại với id: " + requestId));
        if (appealRequest.getRequestType() != RequestType.APPEAL) {
            throw new BadRequestException("Đây không phải là đơn khiếu nại kết quả.");
        }
        if (appealRequest.getStatus() != RequestStatus.IN_REVIEW
        && appealRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Đơn khiếu nại này chưa được ban giám khảo hoàn thành.");
        }
        System.out.println("Status = " + appealRequest.getStatus());

        appealRequest.setStatus(RequestStatus.REJECTED);
        appealRequest.setResponseMessage(responseMessage != null ? responseMessage : "BTC từ chối đơn khiếu nại do điểm số không thay đổi.");
        appealRequest.setResponder(account);
        appealRequest.setResponseAt(LocalDateTime.now());
        auditService.saveLog(
                account,
                AuditAction.REJECT_APPEAL_REQUEST,
                AuditEntityType.TEAM_REQUEST,
                appealRequest.getRequestId(),
                "BTC đã từ chối yêu cầu khiếu nại của team"
        );
        try {
            TeamRequest updated = teamRequestRepository.save(appealRequest);
            return toResponse(updated, null, null);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Đơn khiếu nại này vừa mới được một thành viên BTC khác xử lý mất rồi!");
        }
    }


    // Chấp nhận là khi có sự thay đổi về điểm số
    private TeamRequestResponse acceptAppealRequest(CustomUserDetails userDetails, Integer requestId, String responseMessage) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        TeamRequest appealRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại với id: " + requestId));
        if (appealRequest.getRequestType() != RequestType.APPEAL) {
            throw new BadRequestException("Đây không phải là đơn khiếu nại kết quả.");
        }

        if (appealRequest.getStatus() != RequestStatus.IN_REVIEW) {
            throw new BadRequestException("Đơn khiếu nại này chưa hoàn thành quá trình  đánh giá lại từ giám khảo.");
        }
        appealRequest.setStatus(RequestStatus.RESOLVED);
        appealRequest.setResponseMessage(responseMessage != null ? responseMessage : "BTC đã chấp nhận đơn khiếu nại sau khi có sự thay đổi về điểm số.");
        appealRequest.setResponder(account);
        appealRequest.setResponseAt(LocalDateTime.now());
        auditService.saveLog(
                account,
                AuditAction.ACCEPT_APPEAL_REQUEST,
                AuditEntityType.TEAM_REQUEST,
                appealRequest.getRequestId(),
                "BTC đã chấp nhận yêu cầu khiếu nại của team"
        );
        try {
            TeamRequest updated = teamRequestRepository.save(appealRequest);
            return toResponse(updated, null, null);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Đơn khiếu nại này vừa mới được một thành viên BTC khác xử lý mất rồi!");
        }
    }

    // KHI Event gửi yêu cầu đến Judge chấm lại
    // Thì trạng thái EVALUATION của nó đang ở GRADED Chuyển sang RE_EVALUATION

    private TeamRequestResponse requestExpertToReEvaluation(CustomUserDetails userDetails, Integer requestId) {
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

        TeamParticipant participant = appealRequest.getTeam().getRegistrations().stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(teamParticipants -> teamParticipants != null
                        && teamParticipants.getCategoryRound() != null
                        && teamParticipants.getCategoryRound().getRound()
                        .getRoundId().equals(appealRequest.getRound().getRoundId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy CategoryRound mà team tham gia trong vòng thi này."));

        CategoryRound categoryRound = participant.getCategoryRound();
        Submission finalSubmission = participant.getSubmissions().stream()
                .filter(Submission::isFinal)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Đội thi chưa có bài nộp chính thức trong vòng này."));

        List<Evaluation> evaluationsToUpdate = finalSubmission.getEvaluations().stream()
                .filter(evaluation -> evaluation.getExpertAssign() != null)
                .filter(evaluation -> evaluation.getExpertAssign().getCategoryRound() != null)
                .filter(evaluation -> evaluation.getExpertAssign().getCategoryRound()
                        .getCategoryRoundId() == categoryRound.getCategoryRoundId())
                .filter(evaluation -> evaluation.getExpertAssign().getRole() == ExpertRole.CORE_JUDGE
                        || evaluation.getExpertAssign().getRole() == ExpertRole.GUEST_JUDGE)
                .toList();

        if (evaluationsToUpdate.isEmpty()) {
            throw new BadRequestException(
                    "Không tìm thấy bảng chấm của judge được phân công cho CategoryRound này.");
        }

        Set<Account> expertsToNotify = new HashSet<>();
        for (Evaluation evaluation : evaluationsToUpdate) {
            evaluation.setStatus(EvaluationStatus.RE_EVALUATION);
            expertsToNotify.add(evaluation.getExpertAssign().getExpert().getAccount());
        }

        evaluationRepository.saveAll(evaluationsToUpdate);

        participant.setStatus(ParticipantStatus.RE_EVALUATING);
        participantRepository.save(participant);

        appealRequest.setStatus(RequestStatus.PROCESSING);
        appealRequest.setResponder(account);
        appealRequest.setResponseAt(LocalDateTime.now());
        TeamRequest updated = teamRequestRepository.save(appealRequest);

        notificationService.notifyExpertReEvaluation(account, expertsToNotify, appealRequest.getTeam().getTeamName());
        auditService.saveLog(
                account,
                AuditAction.REQUEST_RE_EVALUATION,
                AuditEntityType.TEAM_REQUEST,
                appealRequest.getRequestId(),
                "BTC phê duyệt đơn phúc khảo và đã chuyển trạng thái đơn sang IN_REVIEW và gửi yêu cầu chấm lại cho ban giám khảo."
        );
        return toResponse(updated, null, null);
    }

    // Khi có yêu cầu phúc khảo từ các bài đánh giá của mình. Ban giám khảo nhận danh sách bài nộp của đội mình đã chấm .
    // Tiến hành xem xét lại và chấm điểm lại.
    @Override
    public Page<TeamRequestResponse> getAppealRequestsForJudge(
            CustomUserDetails userDetails,
            Integer roundId,
            Pageable pageable
    ) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản của Expert, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        //  Lấy tất cả đơn khiếu nại kết quả của vòng đấu này đang ở trạng thái INREVIEW
        Page<TeamRequest> requestPage = teamRequestRepository
                .findByRound_RoundIdAndRequestTypeAndStatus(
                        roundId,
                        RequestType.APPEAL,
                        RequestStatus.PROCESSING,
                        pageable
                );
        if (requestPage.isEmpty()) {
            throw new BadRequestException(
                    "Hiện tại bạn không có đơn khiếu nại nào của vòng đấu này cần rà soát.");
        }
        List<TeamRequestResponse> result = new ArrayList<>();

        for (TeamRequest request : requestPage.getContent()) {

            List<Submission> submissionList = request.getTeam()
                    .getRegistrations()
                    .stream()
                    .filter(reg -> reg.getStatus() == RegistrationStatus.APPROVED)
                    .map(Registration::getParticipants)
                    .flatMap(List::stream)
                    .filter(tp -> tp.getCategoryRound() != null
                            && tp.getCategoryRound().getRound().getRoundId().equals(roundId))
                    .map(TeamParticipant::getSubmissions)
                    .flatMap(List::stream)
                    .toList();
            CategoryRound nameCategoryRound = null;
            List<EvaluationResponse> evaluationResponseList = new ArrayList<>();

            for (Submission submission : submissionList) {
                if (submission.getEvaluations() == null) continue;
                for (Evaluation evaluation : submission.getEvaluations()) {
                    if (evaluation.getExpertAssign() != null
                            && evaluation.getExpertAssign().getExpert() != null
                            && evaluation.getExpertAssign().getExpert().getExpertId() == expert.getExpertId()) {

                        if (nameCategoryRound == null) {
                            nameCategoryRound = submission.getTeamParticipant().getCategoryRound();
                        }

                        List<FileDTO> fileDTOList = submission.getFiles().stream()
                                .map(file -> new FileDTO(
                                        file.getFileName(),
                                        file.getFileUrl()
                                ))
                                .toList();
                        SubmissionResponse response = SubmissionResponse.builder()
                                .submissionId(evaluation.getSubmission().getSubmissionId())
                                .teamName(evaluation.getSubmission().getTeam().getTeamName())
                                .githubUrl(evaluation.getSubmission().getGithubUrl()).
                                fileDTOList(fileDTOList)
                                .build();


                        List<EvaluationDetailResponse> detailResponseList = new ArrayList<>();

                        for (EvaluationDetail detail : evaluation.getEvaluationDetails()) {
                            EvaluationDetailResponse detailResponse = EvaluationDetailResponse.builder()
                                    .evaluationDetailId(detail.getId())
                                    .score(detail.getScore())
                                    .comment(detail.getComment()).build();
                            detailResponseList.add(detailResponse);
                        }
                        EvaluationResponse evaluationResponse = EvaluationResponse.builder()
                                .evaluationId(evaluation.getEvaluationId())
                                .totalScore(evaluation.getScore())
                                .status(evaluation.getStatus())
                                .comment(evaluation.getComment())
                                .submissions(response)
                                .listEvaluationDetail(detailResponseList).build();
                        evaluationResponseList.add(evaluationResponse);

                    }
                }
            }
            if (!evaluationResponseList.isEmpty()) {
                TeamRequestResponse response = toResponse(
                        request, nameCategoryRound, expert.getExpertId());
                response.setListEvaluation(evaluationResponseList);

                result.add(response);
            }
        }
        if (result.isEmpty()) {
            throw new BadRequestException(
                    "Vòng đấu này có đơn khiếu nại cần rà soát, nhưng không có bài nộp nào do bạn chấm ban đầu.");
        }

        return new PageImpl<>(
                result,
                pageable,
                requestPage.getTotalElements()
        );
    }

    @Override
    @Transactional
    public TeamRequestResponse processRequest(CustomUserDetails userDetails, Integer requestId,
                                              ProcessTeamRequest command) {
        TeamRequest teamRequest = teamRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        if (teamRequest.getRequestType() == null) {
            throw new BadRequestException("Yêu cầu chưa được xác định loại xử lý");
        }

        return switch (teamRequest.getRequestType()) {
            case APPEAL -> processAppealRequest(userDetails, requestId, command);
            case DRAW_RESULT_VERIFICATION ->
                    processDrawResultVerification(userDetails, teamRequest, command);
            default -> throw new BadRequestException(
                    "Loại yêu cầu này không được xử lý tại chức năng này");
        };
    }

    private TeamRequestResponse processAppealRequest(CustomUserDetails userDetails, Integer requestId, ProcessTeamRequest command) {
        return switch (command.getAction()) {
            case REQUEST_RE_EVALUATION -> requestExpertToReEvaluation(userDetails, requestId);
            case ACCEPT -> acceptAppealRequest(
                    userDetails, requestId, command.getResponseMessage());
            case REJECT -> rejectAppealRequest(
                    userDetails, requestId, command.getResponseMessage());
            case UPDATE_DRAW_RESULT -> throw new BadRequestException(
                    "Khiếu nại điểm không hỗ trợ cập nhật kết quả bốc thăm");
            case RESOLVE -> throw new BadRequestException(
                    "Khiếu nại điểm sử dụng ACCEPT hoặc REJECT để đưa ra kết luận cuối");
        };
    }

    private TeamRequestResponse processDrawResultVerification(CustomUserDetails userDetails,
                                                              TeamRequest teamRequest,
                                                              ProcessTeamRequest command) {
        Account account = userDetails.getAccount();
        eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải ban tổ chức nên không có quyền xử lý yêu cầu này"));

        if (teamRequest.getStatus() == RequestStatus.RESOLVED
                || teamRequest.getStatus() == RequestStatus.REJECTED
                || teamRequest.getStatus() == RequestStatus.CANCELLED) {
            throw new BadRequestException("Yêu cầu xác thực này đã được đóng");
        }
        if (command.getResponseMessage() == null
                || command.getResponseMessage().isBlank()) {
            throw new BadRequestException("Nội dung phản hồi không được để trống");
        }

        switch (command.getAction()) {
            case UPDATE_DRAW_RESULT -> {
                if (teamRequest.getStatus() != RequestStatus.PENDING
                        && teamRequest.getStatus() != RequestStatus.IN_REVIEW) {
                    throw new BadRequestException(
                            "Chỉ có thể cập nhật kết quả khi yêu cầu đang chờ hoặc đang xem xét");
                }
                teamRequestValidator.validateDrawResultUpdate(
                        teamRequest, command);
                luckyDrawResultService.updateDrawResults(
                        command.getEventId(),
                        command.getDrawResults(),
                        userDetails
                );
                teamRequest.setStatus(RequestStatus.IN_REVIEW);
            }
            case RESOLVE -> {
                if (teamRequest.getStatus() != RequestStatus.IN_REVIEW) {
                    throw new BadRequestException(
                            "Chỉ có thể hoàn tất sau khi kết quả bốc thăm đã được cập nhật");
                }
                teamRequest.setStatus(RequestStatus.RESOLVED);
            }
            case ACCEPT -> {
                if (teamRequest.getStatus() != RequestStatus.PENDING) {
                    throw new BadRequestException(
                            "Chỉ có thể xác nhận trực tiếp yêu cầu đang chờ xử lý");
                }
                teamRequest.setStatus(RequestStatus.RESOLVED);
            }
            case REJECT -> teamRequest.setStatus(RequestStatus.REJECTED);
            case REQUEST_RE_EVALUATION -> throw new BadRequestException(
                    "Yêu cầu xác thực kết quả bốc thăm không thể chuyển cho giám khảo");
        }
        teamRequest.setResponder(account);
        teamRequest.setResponseMessage(command.getResponseMessage());
        teamRequest.setResponseAt(LocalDateTime.now());

        TeamRequest updated = teamRequestRepository.save(teamRequest);

        if (updated.getStatus() == RequestStatus.RESOLVED
                || updated.getStatus() == RequestStatus.REJECTED) {
            Student leader = updated.getTeam().getTeamMembers().stream()
                    .filter(TeamMember::getIsLeader)
                    .map(TeamMember::getStudent)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy leader của team"));

            notificationService.notiResolvedRequest(
                    account, leader.getAccount(), updated.getTeam().getTeamName());
        }

        return toResponse(updated, null, null);
    }

    @Override
    @Transactional
    public TeamRequestResponse createDirectRequest(
            CustomUserDetails userDetails,
            CreateDirectTeamRequest command
    ) {
        Account account = userDetails.getAccount();
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException(
                    "Chỉ tài khoản student mới có thể tạo yêu cầu");
        }

        Team team = teamRepository.findActiveLeadingTeamByStudentId(
                        account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải leader của team đang tham gia"));

        return switch (command.getRequestType()) {
            case APPEAL -> createDirectAppeal(team, command);
            case DRAW_RESULT_VERIFICATION ->
                    createDirectDrawVerification(team, command);
            default -> throw new BadRequestException(
                    "Chỉ hỗ trợ APPEAL và DRAW_RESULT_VERIFICATION");
        };
    }

    private TeamRequestResponse createDirectAppeal(
            Team team, CreateDirectTeamRequest command) {
        if (command.getRoundId() == null) {
            throw new BadRequestException(
                    "Round id không được để trống khi khiếu nại điểm");
        }

        Round round = roundRepository.findById(command.getRoundId())
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy vòng thi"));

        boolean belongsToRound = team.getRegistrations().stream()
                .filter(registration ->
                        registration.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .anyMatch(participant -> participant.getCategoryRound() != null
                        && participant.getCategoryRound().getRound()
                        .getRoundId().equals(round.getRoundId()));
        if (!belongsToRound) {
            throw new BadRequestException(
                    "Team không tham gia vòng thi này");
        }

        Notification sourceNotification = findInitialResultNotification(
                team, round, RequestType.APPEAL);

        return saveDirectRequest(
                team,
                round,
                RequestType.APPEAL,
                command.getRequestMessage(),
                sourceNotification
        );
    }

    private TeamRequestResponse createDirectDrawVerification(
            Team team, CreateDirectTeamRequest command) {
        if (command.getEventId() == null) {
            throw new BadRequestException(
                    "Event id không được để trống khi xác thực kết quả bốc thăm");
        }

        TeamParticipant participant = team.getRegistrations().stream()
                .filter(registration ->
                        registration.getStatus() == RegistrationStatus.APPROVED)
                .filter(registration -> registration.getHackathonEvent() != null
                        && registration.getHackathonEvent().getEventId()
                        == command.getEventId())
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(item -> item.getCategoryRound() != null)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Team chưa có kết quả bốc thăm trong event này"));

        Round round = participant.getCategoryRound().getRound();
        Notification sourceNotification = findInitialResultNotification(
                team, round, RequestType.DRAW_RESULT_VERIFICATION);

        return saveDirectRequest(
                team,
                round,
                RequestType.DRAW_RESULT_VERIFICATION,
                command.getRequestMessage(),
                sourceNotification
        );
    }

    private Notification findInitialResultNotification(
            Team team,
            Round round,
            RequestType requestType
    ) {
        NotificationType notificationType =
                requestType == RequestType.APPEAL
                        ? NotificationType.SUBMISSION_SCORED
                        : NotificationType.ASSIGNED_CATEGORY;

        Account leaderAccount = team.getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .map(Student::getAccount)
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy leader của team"));

        Notification notification = notificationRepository
                .findFirstByAccount_AccountIdAndTeam_TeamIdAndRound_RoundIdAndTypeOrderByCreatedAtAsc(
                        leaderAccount.getAccountId(),
                        team.getTeamId(),
                        round.getRoundId(),
                        notificationType
                )
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy notification kết quả ban đầu"));

        if (notification.getResponseDeadline() == null
                || LocalDateTime.now().isAfter(notification.getResponseDeadline())) {
            throw new BadRequestException(
                    "Đã hết thời hạn gửi yêu cầu");
        }

        return notification;
    }

    private TeamRequestResponse saveDirectRequest(
            Team team,
            Round round,
            RequestType requestType,
            String requestMessage,
            Notification sourceNotification
    ) {
        boolean hasOpenRequest =
                teamRequestRepository
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
        if (hasOpenRequest) {
            throw new BadRequestException(
                    "Team đã có một yêu cầu cùng loại đang được xử lý");
        }

        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setTeam(team);
        teamRequest.setRound(round);
        teamRequest.setRequestType(requestType);
        teamRequest.setRequestMessage(requestMessage);
        teamRequest.setCreateDate(LocalDateTime.now());
        teamRequest.setStatus(RequestStatus.PENDING);
        teamRequest.setSourceNotification(sourceNotification);

        return toResponse(
                teamRequestRepository.save(teamRequest), null, null);
    }

    private TeamRequestResponse toResponse(
            TeamRequest request,
            CategoryRound categoryRound,
            Integer expertId
    ) {
        return TeamRequestResponse.builder()
                .requestId(request.getRequestId())
                .requestType(request.getRequestType())
                .teamId(request.getTeam().getTeamId())
                .teamName(request.getTeam().getTeamName())
                .expertId(expertId)
                .createDate(request.getCreateDate())
                .status(request.getStatus())
                .responseAt(request.getResponseAt())
                .round(categoryRound != null
                        ? categoryRound.getRound().getRoundName()
                        : request.getRound() != null
                          ? request.getRound().getRoundName()
                          : "N/A")
                .categoryName(categoryRound != null
                        ? categoryRound.getCategory().getCategoryName()
                        : "N/A")
                .requestMessage(request.getRequestMessage())
                .responseMessage(request.getResponseMessage())
                .build();
    }




}

