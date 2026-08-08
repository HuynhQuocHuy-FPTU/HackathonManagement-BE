package com.hackathon.service.impl;

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
import com.hackathon.service.NotificationService;
import com.hackathon.service.TeamRequestService;
import com.hackathon.validator.TeamRequestValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
// Quản lý các yêu cầu của đội gồm hỗ trợ cố vấn, khiếu nại và xác minh kết quả bốc thăm.
public class TeamRequestServiceImpl implements TeamRequestService {
    private final TeamRepository teamRepository;
    private final ExpertRepository expertRepository;
    private final ExpertAssignRepository expertAssignRepository;
    private final RoundRepository roundRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EvaluationRepository evaluationRepository;
    private final AuditService auditService;
    private final TeamRequestRepository teamRequestRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final ParticipantRepository participantRepository;
    private final TeamRequestValidator teamRequestValidator;
    private final RoundAdvancementServiceImpl roundAdvancementServiceImpl;
    private final HackathonEventRepository hackathonEventRepository;

    // Tạo TeamRequest khi leader phản hồi trực tiếp một Notification.
    // <p>
    // Luồng xử lý:
    // 1. Kiểm tra tài khoản đăng nhập là sinh viên.
    // 2. Lấy notification và xác nhận tài khoản có quyền phản hồi.
    // 3. Với ASSIGNED_CATEGORY: tạo DRAW_RESULT_VERIFICATION.
    // 4. Với RANKING_DRAFT: tạo APPEAL cho round tương ứng.
    // 5. Lưu TeamRequest, đóng quyền phản hồi notification và lưu nội dung
    // phản hồi của sinh viên.
    //
    // @param notificationId notification được sinh viên phản hồi
    // @return TeamRequest vừa được tạo ở trạng thái PENDING
    @Override
    @Transactional
    public TeamRequestResponse respondNotification(
            CustomUserDetails userDetails,
            Long notificationId,
            NotiResponseRequest command
    ) {
        Account account = teamRequestValidator.requireStudentAccount(userDetails);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy thông báo"));

        teamRequestValidator.validateNotificationResponse(
                notification, account);

        TeamRequest teamRequest = switch (notification.getType()) {
            case ASSIGNED_CATEGORY -> buildDrawVerificationRequest(
                    notification, account, command.getMessage());
            case RANKING_DRAFT -> buildAppealRequest(notification, account, command);
            default -> throw new BadRequestException(
                    "Thông báo này không hỗ trợ tạo TeamRequest");
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

    //---------------------------------------------//
    // TEAM YÊU CẦU SỰ HỔ TRỢ TỪ MENTOR
    //---------------------------------------------//

    // Leader gửi yêu cầu hỗ trợ đến mentor phụ trách CategoryRound hiện tại.
    // <p>
    // Hàm kiểm tra tài khoản là student, student là leader của team đang thi,
    // team chưa có MENTOR_SUPPORT đang chờ, round đang ONGOING và
    // CategoryRound đã có mentor được phân công. Sau khi lưu request, hệ thống
    // gửi notification cho mentor và ghi audit log.
    //
    // @return danh sách gồm yêu cầu hỗ trợ vừa tạo
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
        newRequest.setRequestType(RequestType.MENTOR_SUPPORT);
        TeamRequest saveTeam = teamRequestRepository.save(newRequest);

        notificationService.notifyMentorSupportTeam(account, expertAssign);
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

    // Lấy các yêu cầu MENTOR_SUPPORT mà expert hiện tại có thể xử lý.
    // <p>
    // Ngoài dữ liệu request, response còn chứa CategoryRound hiện tại và số
    // yêu cầu mentor đã chấp nhận/từ chối trong round để phục vụ hiển thị.
    @Override
    public List<TeamRequestResponse> getTeamRequestsForExpert(Integer roundId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin Expert tương ứng với tài khoản này."));
        // Lấy ds các Team gửi request
        List<TeamRequest> requests =
                teamRequestRepository.findRequestForExpertRoleMentor(
                        expert.getExpertId());

        long acceptedRequest = teamRequestRepository.countByRound_RoundIdAndRequestTypeAndStatusAndExpertAssign_Expert_ExpertId(roundId,
                RequestType.MENTOR_SUPPORT, RequestStatus.RESOLVED, expert.getExpertId());

        long rejectedRequest = teamRequestRepository.countByRound_RoundIdAndRequestTypeAndStatusAndExpertAssign_Expert_ExpertId(roundId,
                RequestType.MENTOR_SUPPORT, RequestStatus.REJECTED, expert.getExpertId());


        return requests.stream().map(rq -> {
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
                    .acceptedRequests(acceptedRequest)
                    .rejectedRequests(rejectedRequest)
                    .responseAt(rq.getResponseAt())
                    .responseMessage(rq.getResponseMessage())
                    .build();
        }).toList();
    }

    //-------------------------------------//
    // MENTOR: CHẤP NHẬN VÀ TỪ CHỐI YÊU CẦU TỪ ĐỘI THI
    //-------------------------------------//

    // Mentor tiếp nhận một yêu cầu MENTOR_SUPPORT đang ở trạng thái PENDING.
    // <p>
    // Hàm kiểm tra expert thuộc CategoryRound của team và request chưa bị
    // mentor khác nhận. Khi hợp lệ, request được gắn ExpertAssign, cập nhật
    // trạng thái xử lý, gửi notification cho leader và ghi audit log.
    @Override
    @Transactional
    public TeamRequestResponse acceptTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là Chuyên gia vì vậy không được phép truy cập vào trình duyệt này."));
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

        teamRequest.setStatus(RequestStatus.RESOLVED);
        teamRequest.setResponseAt(LocalDateTime.now());
        teamRequest.setExpertAssign(mySpecificAssign);
        teamRequest.setResponseMessage(responseMessage);
        teamRequest.setResponder(account);
        Account teamLeader = teamRequest.getTeam().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(tm -> tm.getStudent().getAccount())
                .findFirst().orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin trưởng nhóm."));
        notificationService.notifyMentorResponseSupportTeam(teamLeader, account, responseMessage, true);

        auditService.saveLog(
                account,
                AuditAction.MENTOR_ACCEPT_REQUEST,
                AuditEntityType.TEAM,
                teamRequest.getTeam().getTeamId(),
                "Chấp nhận yêu cầu hỗ trợ từ team thành công"
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


    // Mentor từ chối một yêu cầu MENTOR_SUPPORT đang chờ.
    // <p>
    // Chỉ mentor có phân công phù hợp mới được từ chối. Request được cập nhật
    // trạng thái REJECTED, lưu lý do phản hồi, thông báo cho leader và ghi log.
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
        teamRequest.setResponseMessage(responseMessage);
        teamRequest.setResponder(account);
        if (responseMessage == null || responseMessage.isEmpty()) {
            teamRequest.setResponseMessage("Yêu cầu đã được từ chối");
        } else {
            teamRequest.setResponseMessage(responseMessage);
        }
        Account teamLeader = teamRequest.getTeam().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(tm -> tm.getStudent().getAccount())
                .findFirst().orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin trưởng nhóm."));
        notificationService.notifyMentorResponseSupportTeam(teamLeader, account, responseMessage, false);


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

    // Ban tổ chức lấy danh sách đơn khiếu nại trong một vòng thi.
    @Override
    public List<TeamRequestResponse> getAppealRequest(
            CustomUserDetails userDetails,
            Integer roundId
    ) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));

        // Lấy ds đơn khiếu nại
        List<TeamRequest> appealRequests = teamRequestRepository
                .findByRound_RoundIdAndRequestType(
                        roundId, RequestType.APPEAL);
        if (appealRequests.isEmpty()) {
            throw new BadRequestException(
                    "Không tìm thấy đơn khiếu nại của round id: " + roundId);
        }

        return appealRequests.stream().map(rq -> TeamRequestResponse.builder()
                .requestId(rq.getRequestId())
                .teamId(rq.getTeam().getTeamId())
                .teamName(rq.getTeam().getTeamName())
                .createDate(rq.getCreateDate())
                .status(rq.getStatus())
                .round(rq.getRound().getRoundName())
                .requestMessage(rq.getRequestMessage())
                .responseMessage(rq.getResponseMessage())
                .responseAt(rq.getResponseAt())
                .build()).toList();
    }

    // Lấy toàn bộ yêu cầu thuộc một sự kiện cho tài khoản có thẩm quyền.
    @Override
    public List<TeamRequestResponse> getAllRequestsForEvent(
            CustomUserDetails userDetails,
            Integer eventId
    ) {
        Account account = userDetails.getAccount();
        if (account == null) {
            throw new BadRequestException("Không tìm thấy tài khoản");
        }

        eventCoordinatorRepository.findByAccount_AccountId(
                        account.getAccountId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải Event Coordinator"));

        return teamRequestRepository.findByRound_HackathonEvent_EventIdAndRequestTypeNotOrderByCreateDateDesc(eventId, RequestType.MENTOR_SUPPORT
                )
                .stream()
                .map(request -> toResponse(request, null, null))
                .toList();
    }

    // Lấy danh sách đơn khiếu nại được phép công khai của một vòng thi.
    @Override
    public List<TeamRequestResponse> getAppealRequestPublic(
            CustomUserDetails userDetails,
            Integer roundId
    ) {
        Account account = userDetails.getAccount();
        if (account == null) {
            throw new BadRequestException("Account không tồn tại");
        }
        List<TeamRequest> appealRequests =
                teamRequestRepository.findByRound_RoundId(roundId);
        if (appealRequests.isEmpty()) {
            throw new BadRequestException(
                    "Không tìm thấy đơn khiếu nại của round id: " + roundId);
        }

        return appealRequests.stream().map(rq -> TeamRequestResponse.builder()
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
                .build()).toList();
    }


    // Chấp nhận là khi có sự thay đổi về điểm số
    // KHI Event gửi yêu cầu đến Judge chấm lại
    // Thì trạng thái EVALUATION của nó đang ở GRADED Chuyển sang RE_EVALUATION

    // Khi có yêu cầu phúc khảo từ các bài đánh giá của mình. Ban giám khảo nhận danh sách bài nộp của đội mình đã chấm .
    // Tiến hành xem xét lại và chấm điểm lại.

    // Lấy các đơn khiếu nại liên quan đến bài chấm của giám khảo hiện tại.
    public List<TeamRequestResponse> getAppealRequestsForJudge(
            CustomUserDetails userDetails,
            Integer roundId
    ) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản của Chuyên gia, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        //  Lấy tất cả đơn khiếu nại kết quả của vòng đấu này đang ở trạng thái INREVIEW
        List<TeamRequest> requests = teamRequestRepository
                .findByRound_RoundIdAndRequestTypeAndStatus(
                        roundId,
                        RequestType.APPEAL,
                        RequestStatus.PROCESSING
                );
        if (requests.isEmpty()) {
            throw new BadRequestException(
                    "Hiện tại bạn không có đơn khiếu nại nào của vòng đấu này cần rà soát.");
        }
        List<TeamRequestResponse> result = new ArrayList<>();

        for (TeamRequest request : requests) {

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
                                    .criteriaName(detail.getEvaluationCriteria().getCriteriaName())
                                    .score(detail.getScore())
                                    .criteriaType(detail.getEvaluationCriteria().getType())
                                    .weight(detail.getEvaluationCriteria().getWeight())
                                    .criteriaDescription(detail.getEvaluationCriteria().getDescription())
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

        return result;
    }

    // Điểm vào chung để Ban tổ chức xử lý APPEAL hoặc
    // DRAW_RESULT_VERIFICATION.
    // <p>
    // Hàm tải request, kiểm tra RequestType rồi chuyển tiếp:
    // - APPEAL sang processAppealRequest().
    // - DRAW_RESULT_VERIFICATION sang processDrawResultVerification().
    // Các loại request khác không thuộc luồng này và sẽ bị từ chối.
    //
    // @param command action cần thực hiện và nội dung phản hồi
    // @return request sau khi đã cập nhật trạng thái
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
            case DRAW_RESULT_VERIFICATION -> processDrawResultVerification(userDetails, teamRequest, command);
            default -> throw new BadRequestException(
                    "Loại yêu cầu này không được xử lý tại chức năng này");
        };
    }

    // Cho phép leader tạo request mà không phản hồi qua endpoint notification.
    // <p>
    // APPEAL:
    // - Team phải tham gia round.
    // - Phải có RANKING_DRAFT ban đầu.
    // - Thời gian hiện tại phải nằm trong appeal time của round.
    // <p>
    // DRAW_RESULT_VERIFICATION:
    // - Team phải có kết quả phân category trong event.
    // - Phải có ASSIGNED_CATEGORY ban đầu.
    // - Dùng response deadline của notification để kiểm tra thời hạn.
    @Override
    @Transactional
    public TeamRequestResponse createDirectRequest(
            CustomUserDetails userDetails,
            CreateDirectTeamRequest command
    ) {
        Account account = teamRequestValidator.requireStudentAccount(userDetails);
        Team team = teamRequestValidator.requireActiveLeadingTeam(account);

        return switch (command.getRequestType()) {
            case APPEAL -> createDirectAppeal(
                    team, account.getAccountId(), command);
            case DRAW_RESULT_VERIFICATION -> createDirectDrawVerification(
                    team, account.getAccountId(), command);
            default -> throw new BadRequestException(
                    "Chỉ hỗ trợ khiếu nại (APPEAL) và  xác thực kết quả (DRAW_RESULT_VERIFICATION)");
        };
    }

    // Sinh viên xem các yêu cầu hỗ trợ mentor của team trong sự kiện.
    @Override
    public List<TeamRequestResponse> getMyMentorSupportRequests(CustomUserDetails userDetails, Integer eventId) {
        return getMyRequests(
                userDetails,
                eventId,
                List.of(RequestType.MENTOR_SUPPORT));
    }

    // Sinh viên xem các đơn khiếu nại của team trong sự kiện.
    @Override
    public List<TeamRequestResponse> getMyAppealRequests(CustomUserDetails userDetails, Integer eventId) {
        return getMyRequests(
                userDetails,
                eventId,
                List.of(
                        RequestType.APPEAL,
                        RequestType.DRAW_RESULT_VERIFICATION));
    }

    // Lấy lịch sử yêu cầu của đội theo sự kiện và các loại yêu cầu được phép xem.
    private List<TeamRequestResponse> getMyRequests(
            CustomUserDetails userDetails,
            Integer eventId,
            List<RequestType> requestTypes
    ) {
        Account account = teamRequestValidator.requireStudentAccount(userDetails);
        
        if (eventId != null && eventId > 0) {
            if (!hackathonEventRepository.existsById(eventId)) {
                throw new BadRequestException("Không tìm thấy sự kiện");
            }
            return teamRequestRepository.findMyRequestsByEventAndTypes(
                            eventId,
                            requestTypes,
                            account.getStudent().getStudentId())
                    .stream()
                    .map(request -> toResponse(request, null, null))
                    .toList();
        } else {
            return teamRequestRepository.findAllMyRequestsByTypes(
                            requestTypes,
                            account.getStudent().getStudentId())
                    .stream()
                    .map(request -> toResponse(request, null, null))
                    .toList();
        }
    }

    //---------------------------------------------//
    // PRIVATE HELPERS
    //---------------------------------------------//

    // Dựng DRAW_RESULT_VERIFICATION từ notification ASSIGNED_CATEGORY.
    // <p>
    // Hàm tìm team mà tài khoản đang là leader và team đó chính là team nhận
    // notification. Sau đó kiểm tra không có request cùng loại đang mở và team
    // thực sự đã có TeamParticipant được gán CategoryRound. Hàm chỉ dựng entity,
    // việc lưu request và cập nhật notification do respondNotification() làm.
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
                        "Bạn không phải leader của đội nhận kết quả bốc thăm"));

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

    // Dựng APPEAL từ notification RANKING_DRAFT.
    // <p>
    // Round ID bắt buộc phải có, phải trùng với round của notification và
    // tài khoản phải là leader của một team có participant trong round đó.
    // Cuối cùng kiểm tra team chưa có APPEAL đang mở trước khi dựng request.
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
                    "Thông báo điểm không thuộc vòng thi này");
        }

        Team team = teamRequestValidator.requireActiveLeadingTeam(account);
        teamRequestValidator.requireParticipantInRound(team, round);

        teamRequestValidator.validateNoOpenRequest(
                team, round, RequestType.APPEAL);

        return newPendingRequest(
                team, round, RequestType.APPEAL, command.getMessage());
    }

    // Hàm dùng chung để tạo TeamRequest chưa lưu với các trường cơ bản:
    // team, round, request type, nội dung, thời gian tạo và trạng thái PENDING.
    // Các thông tin nguồn như sourceNotification sẽ được caller gắn sau.
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

    // Ban tổ chức từ chối và đóng đơn khiếu nại.
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

        // 4. Gửi thông báo
        Student teamLeader = appealRequest.getTeam().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Leader"));
        notificationService.notifyResponseAppeal(account, teamLeader.getAccount(), appealRequest.getTeam().getTeamName(), true);
        auditService.saveLog(
                account,
                AuditAction.REJECT_APPEAL_REQUEST,
                AuditEntityType.TEAM_REQUEST,
                appealRequest.getRequestId(),
                "BTC đã từ chối yêu cầu khiếu nại của team"
        );

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

    // Ban tổ chức chấp nhận kết quả xử lý và hoàn tất đơn khiếu nại.
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

        Student teamLeader = appealRequest.getTeam().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Leader"));
        notificationService.notifyResponseAppeal(account, teamLeader.getAccount(), appealRequest.getTeam().getTeamName(), true);

        auditService.saveLog(
                account,
                AuditAction.ACCEPT_APPEAL_REQUEST,
                AuditEntityType.TEAM_REQUEST,
                appealRequest.getRequestId(),
                "BTC đã chấp nhận yêu cầu khiếu nại của team"
        );

        auditService.saveLog(
                account,
                AuditAction.ACCEPT_APPEAL_REQUEST,
                AuditEntityType.TEAM_REQUEST,
                appealRequest.getRequestId(),
                "BTC đã xử lý yêu cầu khiếu nại của team"
        );
        TeamParticipant teamParticipant = participantRepository
                .findReEvaluatingParticipant(
                        appealRequest.getTeam().getTeamId(),
                        appealRequest.getRound().getRoundId())
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy đội đang được phúc khảo trong vòng thi của đơn khiếu nại."));
        //tính lại điểm và ranking cho category sao khi chấm điểm lại
        roundAdvancementServiceImpl.calculateScoresAndRanking(teamParticipant.getCategoryRound().getCategoryRoundId());
        teamParticipant.setStatus(ParticipantStatus.ACTIVE);
        participantRepository.save(teamParticipant);

        try {
            TeamRequest updated = teamRequestRepository.save(appealRequest);

            Account leaderAccount = updated.getTeam().getTeamMembers().stream()
                    .filter(TeamMember::getIsLeader)
                    .map(TeamMember::getStudent)
                    .map(Student::getAccount)
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy leader của team"));

            notificationService.notifyTeamRequestResolved(
                    account,
                    leaderAccount,
                    updated.getTeam().getTeamName(),
                    updated.getRequestType()
            );

            return toResponse(updated, null, null);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Đơn khiếu nại này vừa mới được một thành viên BTC khác xử lý mất rồi!");
        }
    }

    // Đổi trạng thái của evaluation và gửi noti cho judge chấm category đó.
    private TeamRequestResponse requestExpertToReEvaluation(CustomUserDetails userDetails, Integer requestId) {
        Account account = userDetails.getAccount();
        EventCoordinator eventCoordinator = eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId()).orElseThrow(() -> new BadRequestException("Bạn không phải là ban tổ chức, vì vậy bạn không được phép truy cập vào trình duyệt này."));
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
        Submission finalSubmission = participant.getSubmissions().stream().filter(Submission::isFinal).findFirst().orElseThrow(() -> new BadRequestException("Đội thi chưa có bài nộp chính thức trong vòng này."));

        List<Evaluation> evaluationsToUpdate = finalSubmission.getEvaluations().stream()
                .filter(evaluation -> evaluation.getExpertAssign() != null)
                .filter(evaluation -> evaluation.getExpertAssign().getCategoryRound() != null)
                .filter(evaluation -> Objects.equals(
                        evaluation.getExpertAssign().getCategoryRound().getCategoryRoundId(),
                        categoryRound.getCategoryRoundId()))
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
            evaluation.setIsReEvaluation(true);
            evaluation.getEvaluationDetails().forEach(
                    detail -> detail.setIsReEvaluation(false));
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
                "BTC phê duyệt đơn phúc khảo và đã chuyển trạng thái đơn sang xem xét và gửi yêu cầu chấm lại cho ban giám khảo."
        );
        return toResponse(updated, null, null);
    }

    // Ánh xạ action của APPEAL tới nghiệp vụ tương ứng:
    // REQUEST_RE_EVALUATION chuyển giám khảo chấm lại;
    // RESOLVE chấp nhận kết quả xử lý;
    // REJECT từ chối đơn;
    // UPDATE_DRAW_RESULT không hợp lệ đối với khiếu nại điểm.
    private TeamRequestResponse processAppealRequest(CustomUserDetails userDetails, Integer requestId, ProcessTeamRequest command) {
        return switch (command.getAction()) {
            case REQUEST_RE_EVALUATION -> requestExpertToReEvaluation(userDetails, requestId);
            case RESOLVE -> acceptAppealRequest(
                    userDetails, requestId, command.getResponseMessage());
            case REJECT -> rejectAppealRequest(
                    userDetails, requestId, command.getResponseMessage());
            case UPDATE_DRAW_RESULT -> throw new BadRequestException(
                    "Khiếu nại điểm không hỗ trợ cập nhật kết quả bốc thăm");
        };
    }

    // Xử lý DRAW_RESULT_VERIFICATION bởi Ban tổ chức.
    // <p>
    // UPDATE_DRAW_RESULT chuyển request sang IN_REVIEW để Ban tổ chức sửa kết
    // quả bên chức năng bốc thăm. RESOLVE đóng request thành công, REJECT từ
    // chối request. REQUEST_RE_EVALUATION bị chặn vì xác minh bốc thăm không
    // liên quan đến giám khảo. Khi request đóng, leader được gửi notification.
    private TeamRequestResponse processDrawResultVerification(CustomUserDetails userDetails, TeamRequest teamRequest, ProcessTeamRequest command) {
        Account account = userDetails.getAccount();
        eventCoordinatorRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải ban tổ chức nên không có quyền xử lý yêu cầu này"));

        if (teamRequest.getStatus() == RequestStatus.RESOLVED
                || teamRequest.getStatus() == RequestStatus.REJECTED) {
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
                teamRequest.setStatus(RequestStatus.IN_REVIEW);
            }
            case RESOLVE -> {
                if (teamRequest.getStatus() != RequestStatus.PENDING
                        && teamRequest.getStatus() != RequestStatus.IN_REVIEW) {
                    throw new BadRequestException(
                            "Chỉ có thể hoàn tất yêu cầu đang chờ hoặc đã được cập nhật");
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

            notificationService.notifyTeamRequestResolved(
                    account,
                    leader.getAccount(),
                    updated.getTeam().getTeamName(),
                    updated.getRequestType()
            );
        }

        return toResponse(updated, null, null);
    }

    // Tạo APPEAL trực tiếp cho một round.
    // <p>
    // Hàm kiểm tra round tồn tại và team có participant thuộc round. Sau đó lấy
    // notification RANKING_DRAFT ban đầu qua findInitialResultNotification().
    // Việc kiểm tra appealStartTime/appealEndTime được thực hiện trong helper
    // đó, không dùng response deadline của notification cho APPEAL.
    private TeamRequestResponse createDirectAppeal(
            Team team,
            Integer leaderAccountId,
            CreateDirectTeamRequest command) {
        if (command.getRoundId() == null) {
            throw new BadRequestException(
                    "Round id không được để trống khi khiếu nại điểm");
        }

        Round round = roundRepository.findById(command.getRoundId())
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy vòng thi"));
        teamRequestValidator.requireParticipantInRound(team, round);

        Notification sourceNotification = findInitialResultNotification(
                leaderAccountId, round, RequestType.APPEAL);

        return saveDirectRequest(
                team,
                round,
                RequestType.APPEAL,
                command.getRequestMessage(),
                sourceNotification
        );
    }

    // Tạo DRAW_RESULT_VERIFICATION trực tiếp trong một event.
    // <p>
    // Hàm tìm registration APPROVED đúng event và participant đã có
    // CategoryRound, từ đó xác định round của kết quả bốc thăm. Request chỉ
    // được tạo nếu tìm thấy notification ASSIGNED_CATEGORY ban đầu còn hạn.
    private TeamRequestResponse createDirectDrawVerification(
            Team team,
            Integer leaderAccountId,
            CreateDirectTeamRequest command) {
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
                leaderAccountId, round,
                RequestType.DRAW_RESULT_VERIFICATION);

        return saveDirectRequest(
                team,
                round,
                RequestType.DRAW_RESULT_VERIFICATION,
                command.getRequestMessage(),
                sourceNotification
        );
    }

    // Tìm notification gốc dùng làm bằng chứng cho direct request.
    // <p>
    // Với APPEAL:
    // - Tìm notification RANKING_DRAFT đầu tiên của leader trong round.
    // - Yêu cầu round đã cấu hình appealStartTime và appealEndTime.
    // - Chỉ cho phép tạo request trong khoảng thời gian khiếu nại của round.
    // <p>
    // Với DRAW_RESULT_VERIFICATION:
    // - Tìm notification ASSIGNED_CATEGORY đầu tiên.
    // - Notification ban đầu là bắt buộc; không có thì không thể xác minh.
    // - Kiểm tra responseDeadline của chính notification này.
    //
    // @return notification gốc hợp lệ để gắn vào sourceNotification
    private Notification findInitialResultNotification(
            Integer leaderAccountId,
            Round round,
            RequestType requestType
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (requestType == RequestType.DRAW_RESULT_VERIFICATION) {
            if (round.getHackathonEvent() == null) {
                throw new BadRequestException(
                        "Không tìm thấy sự kiện của kết quả bốc thăm");
            }
            if (round.getHackathonEvent().getStartDate() != null
                    && !now.isBefore(round.getHackathonEvent().getStartDate())) {
                throw new BadRequestException(
                        "Sự kiện đã bắt đầu, không thể gửi yêu cầu xác minh kết quả bốc thăm");
            }
        }

        NotificationType notificationType =
                requestType == RequestType.APPEAL
                        ? NotificationType.RANKING_DRAFT
                        : NotificationType.ASSIGNED_CATEGORY;

        Notification notification = notificationRepository.findFirstByAccount_AccountIdAndRound_RoundIdAndTypeOrderByCreatedAtAsc(
                        leaderAccountId,
                        round.getRoundId(),
                        notificationType
                )
                .orElseThrow(() -> new BadRequestException(
                        requestType == RequestType.DRAW_RESULT_VERIFICATION
                                ? "Bạn chưa có thông báo về kết quả bốc thăm"
                                : "Không tìm thấy thông báo bảng xếp hạng tạm thời của vòng thi"));

        if (requestType == RequestType.APPEAL) {
            if (round.getAppealStartTime() == null
                    || round.getAppealEndTime() == null) {
                throw new BadRequestException(
                        "Vòng thi chưa thiết lập thời gian nhận đơn khiếu nại");
            }
            if (now.isBefore(round.getAppealStartTime())) {
                throw new BadRequestException(
                        "Chưa đến thời gian gửi đơn khiếu nại");
            }
            if (now.isAfter(round.getAppealEndTime())) {
                throw new BadRequestException(
                        "Đã hết thời hạn gửi đơn khiếu nại");
            }
        } else if (notification.getResponseDeadline() == null) {
            throw new BadRequestException(
                    "Thông báo kết quả bốc thăm ban đầu chưa thiết lập thời hạn xác minh");
        } else if (now.isAfter(notification.getResponseDeadline())) {
            throw new BadRequestException(
                    "Đã hết thời hạn xác minh kết quả bốc thăm theo thông báo kết quả ban đầu");
        }

        return notification;
    }

    // Lưu direct request sau khi kiểm tra chống tạo trùng.
    // <p>
    // Nếu team đã có request cùng round, cùng type ở PENDING, IN_REVIEW hoặc
    // PROCESSING thì từ chối. Nếu không, tạo request PENDING, gắn notification
    // nguồn, lưu repository và chuyển entity thành response.
    private TeamRequestResponse saveDirectRequest(Team team, Round round, RequestType requestType, String requestMessage, Notification sourceNotification
    ) {
        boolean hasOpenRequest =
                teamRequestRepository.existsByTeam_TeamIdAndRound_RoundIdAndStatusInAndRequestType(team.getTeamId(), round.getRoundId(), List.of(RequestStatus.PENDING,
                                RequestStatus.IN_REVIEW,
                                RequestStatus.PROCESSING),
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

