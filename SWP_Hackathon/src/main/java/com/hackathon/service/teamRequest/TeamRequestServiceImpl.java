package com.hackathon.service.teamRequest;

import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.evaluation.EvaluationDetailResponse;
import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.dto.submission.FileDTO;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AuditService;
import com.hackathon.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

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
                .build();
    }


    //---------------------------------------------//
    // TEAM YÊU CẦU SỰ HỔ TRỢ TỪ MENTOR
    //---------------------------------------------//
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
        newRequest.setRequestMessage(requestMessage);
        newRequest.setRequestType(RequestType.MENTOR_SUPPORT);
        TeamRequest saveTeam = teamRequestRepository.save(newRequest);

        auditService.saveLog(
                account,
                AuditAction.SEND_MENTOR_REQUEST,
                AuditEntityType.TEAM,
                team.getTeamId(),
                "Team gửi yêu cầu đến Mentor hỗ trợ thành công"
        );
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
//                    .responseStatus(rq.getResponseStatus())
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

            return mapToResponse(updateRequest, categoryRound, expert.getExpertId());

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Yêu cầu này vừa mới được một Mentor khác xử lý mất rồi!");
        }


    }
    //---------------------------------------//
    // TEAM GỬI KHIẾU NẠI ĐẾN BAN TỔ CHỨC
    //---------------------------------------//

    @Override
    public List<TeamRequestResponse> teamSendAppealRequest(TeamAppealRequestDTO request, CustomUserDetails userDetails) {
        Account account = userDetails.getAccount();

        Student student = studentRepository.findById(account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản sinh viên"));

        Round round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy vòng thi này."));

        LocalDateTime now = LocalDateTime.now();
        if (round.getStatus() != RoundStatus.APPEALING) {
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

        boolean hasPendingRequest = teamRequestRepository.existsByTeam_TeamIdAndStatusAndRequestType(teamMember.getTeam().getTeamId(), RequestStatus.PENDING, RequestType.APPEAL);
        if (hasPendingRequest) {
            throw new BadRequestException("Đội của bạn đã gửi một đơn khiếu nại trước đó và đang chờ xử lý.");
        }
        // Lưu đơn khiếu nại
        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setRequestMessage(request.getRequestMessage());
        teamRequest.setTeam(teamMember.getTeam());
        teamRequest.setRequestType(RequestType.APPEAL);
        teamRequest.setCreateDate(LocalDateTime.now());
        teamRequest.setStatus(RequestStatus.PENDING);
        teamRequest.setRound(round);
        teamRequest.setResponseMessage(null);

        TeamRequest saveTeam = teamRequestRepository.save(teamRequest);
        auditService.saveLog(
                account,
                AuditAction.SEND_APPEAL_REQUEST,
                AuditEntityType.ROUND,
                round.getRoundId(),
                "Team gửi yêu cầu khiếu nại kết quả thành công"
        );
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
        if (appealRequest.getStatus() != RequestStatus.RE_EVALUATED
        && appealRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Đơn khiếu nại này chưa được ban giám khảo hoàn thành.");
        }
        System.out.println("Status = " + appealRequest.getStatus());



        appealRequest.setStatus(RequestStatus.DECLINED);
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
            return mapToResponse(updated, null, null);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Đơn khiếu nại này vừa mới được một thành viên BTC khác xử lý mất rồi!");
        }
    }


    // Chấp nhận là khi có sự thay đổi về điểm số
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

        if (appealRequest.getStatus() != RequestStatus.RE_EVALUATED) {
            throw new BadRequestException("Đơn khiếu nại này chưa hoàn thành quá trình  đánh giá lại từ giám khảo.");
        }

        appealRequest.setStatus(RequestStatus.ACCEPTED);
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
            return mapToResponse(updated, null, null);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new BadRequestException("Đơn khiếu nại này vừa mới được một thành viên BTC khác xử lý mất rồi!");
        }
    }

    // KHI Event gửi yêu cầu đến Judge chấm lại
    // Thì trạng thái EVALUATION của nó đang ở GRADED Chuyển sang RE_EVALUATION
    @Override
    @Transactional
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

        // Lấy bài nộp từ đội thi phúc khảo
        List<Submission> submissions = appealRequest.getTeam().getRegistrations().stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(teamParticipants -> teamParticipants != null
                        && teamParticipants.getCategoryRound() != null
                        && teamParticipants.getCategoryRound().getRound()
                        .getRoundId().equals(appealRequest.getRound().getRoundId()))
                .map(TeamParticipant::getSubmissions)
                .flatMap(List::stream)
                .toList();
        if (submissions.isEmpty()) {
            throw new BadRequestException(
                    "Đội thi chưa có bài nộp trong vòng này.");
        }

        List<Evaluation> evaluationsToUpdate = new ArrayList<>();

        Set<Account> expertsToNotify = new HashSet<>();
        for (Submission submission : submissions) {
            if (submission.getEvaluations() == null) continue;

            for (Evaluation evaluation : submission.getEvaluations()) {
                evaluation.setStatus(EvaluationStatus.RE_EVALUATION);
                evaluationsToUpdate.add(evaluation);


                if (evaluation.getExpertAssign() != null) {
                    expertsToNotify.add(evaluation.getExpertAssign().getExpert().getAccount());
                }

            }
        }

        evaluationRepository.saveAll(evaluationsToUpdate);

        appealRequest.setStatus(RequestStatus.IN_REVIEW);
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
        return mapToResponse(updated, null, null);
    }

    // Khi có yêu cầu phúc khảo từ các bài đánh giá của mình. Ban giám khảo nhận danh sách bài nộp của đội mình đã chấm .
    // Tiến hành xem xét lại và chấm điểm lại.
    @Override
    public List<TeamRequestResponse> getAppealRequestsForJudge(CustomUserDetails userDetails, Integer roundId) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản của Expert, vì vậy bạn không được phép truy cập vào trình duyệt này."));
        //  Lấy tất cả đơn khiếu nại kết quả của vòng đấu này đang ở trạng thái INREVIEW
        List<TeamRequest> listRequest = teamRequestRepository
                .findByRound_RoundIdAndRequestTypeAndStatus(roundId, RequestType.APPEAL, RequestStatus.IN_REVIEW);
        if (listRequest == null || listRequest.isEmpty()) {
            throw new BadRequestException("Hiện tại bạn không có đơn khiếu nại nào của vòng đấu này cần rà soát.");
        }
        List<TeamRequestResponse> result = new ArrayList<>();

        for (TeamRequest request : listRequest) {

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
                                .listSubmission(List.of(response))
                                .listEvaluationDetail(detailResponseList).build();
                        evaluationResponseList.add(evaluationResponse);

                    }
                }
            }
            if (!evaluationResponseList.isEmpty()) {
                TeamRequestResponse response = mapToResponse(request, nameCategoryRound, expert.getExpertId());
                response.setListEvaluation(evaluationResponseList);

                result.add(response);
            }
        }
        if (result.isEmpty()) {
            throw new BadRequestException("Vòng đấu này có đơn khiếu nại cần rà soát, nhưng không có bài nộp nào do bạn chấm ban đầu.");
        }
        return result;
    }

    @Override
    public TeamRequestResponse sendRequestToCoordinator(CustomUserDetails userDetails, String requestMessage, Long notificationId) {
        Account account = userDetails.getAccount();
        if (account == null || account.getStudent() == null) {
            throw new BadRequestException("Tài khoản này không phải là tài khoản student");
        }
        notificationService.checkResponseNoti(notificationId);
        //Tìm Team mà Student này làm leader và đang trạng thái thi đấu
        Team team = teamRepository.findActiveLeadingTeamByStudentId(account.getStudent().getStudentId())
                .orElseThrow(() -> new BadRequestException("Bạn không phải leader của đội tham gia"));

        boolean hasPendingRequest = teamRequestRepository.existsByTeam_TeamIdAndStatusAndRequestType(team.getTeamId(), RequestStatus.PENDING, RequestType.VERIFICATION);
        if (hasPendingRequest) {
            throw new BadRequestException("Đội của bạn đã có một yêu cầu đang nằm trong danh sách chờ. Vui lòng đợi xử lý trước khi gửi yêu cầu mới!");
        }

        TeamRequest newRequest = new TeamRequest();
        newRequest.setTeam(team);
        newRequest.setCreateDate(LocalDateTime.now());
        newRequest.setStatus(RequestStatus.PENDING);
        newRequest.setRequestMessage(requestMessage);
        newRequest.setRequestType(RequestType.VERIFICATION);
        TeamRequest saveTeamRequest = teamRequestRepository.save(newRequest);

        return TeamRequestResponse.builder()
                .requestId(saveTeamRequest.getRequestId())
                .teamId(saveTeamRequest.getTeam().getTeamId())
                .teamName(saveTeamRequest.getTeam().getTeamName())
                .createDate(saveTeamRequest.getCreateDate())
                .status(saveTeamRequest.getStatus())
                .requestMessage(saveTeamRequest.getRequestMessage())
                .build();
    }
    @Transactional
    public void resolvedRequest(CustomUserDetails userDetails, Integer teamRequestId, String messageResponse) {
        // 1. Kiểm tra quyền
        EventCoordinator eventCoordinator = userDetails.getAccount().getEventCoordinator();
        if(eventCoordinator == null) {
            throw new ResourceNotFoundException("Bạn không phải là eventcoordinator");
        }
        // 2. Lấy request và kiểm tra
        TeamRequest teamRequest = teamRequestRepository.findById(teamRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu"));

        if (teamRequest.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý hoặc đã đóng.");
        }
        // 3. Cập nhật thông tin xử lý
        teamRequest.setResponder(userDetails.getAccount());
        teamRequest.setStatus(RequestStatus.ACCEPTED);
        teamRequest.setResponseMessage(messageResponse);
        teamRequestRepository.save(teamRequest);

        // 4. Gửi thông báo
        Student teamLeader = teamRequest.getTeam().getTeamMembers().stream()
                .filter(TeamMember::getIsLeader)
                .map(TeamMember::getStudent)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Leader"));

        notificationService.notiResolvedRequest(userDetails.getAccount(), teamLeader.getAccount(), teamRequest.getTeam().getTeamName());
    }

//    @Override
//    public void reEvaluationSubmission(CustomUserDetails userDetails, ReDetailEvaluationRequest request) {
//        Account account = userDetails.getAccount();
//        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
//                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản của Expert, vì vậy bạn không được phép truy cập vào trình duyệt này."));
//        //  Lấy tất cả đơn khiếu nại kết quả của vòng đấu này đang ở trạng thái INREVIEW
//        TeamRequest appealRequest = teamRequestRepository.findById(request.getRequestId())
//                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại phúc khảo nào."));
//        if (appealRequest.getStatus() != RequestStatus.IN_REVIEW) {
//            throw new BadRequestException("Đơn khiếu nại này không ở trạng thái INREVIEW");
//        }
//        // Từ ds khiếu nại lấy ra bài nộp để tiến hành chấm điểm lại
//        Round round = appealRequest.getRound();
//
//        // 1 team thi nhiều hạng mục sau này sữa lại
//        List<Submission> submissions = appealRequest.getTeam().getRegistrations().stream()
//                .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
//                .map(Registration::getParticipants)
//                .flatMap(List::stream)
//                .filter(teamParticipants -> teamParticipants.getCategoryRound() != null && teamParticipants.getCategoryRound().getRound().getRoundId().equals(round.getRoundId()))
//                .map(TeamParticipant::getSubmissions)
//                .flatMap(List::stream)
//                .toList();
//
//        // Từ  bài nộp tìm ra expert này chấm
//        Evaluation evaluation = null;
//
//        for (Submission submission : submissions) {
//            if (submission.getEvaluations() == null || submission.getEvaluations().isEmpty()) {
//                continue;
//            }
//
//            for (Evaluation eval : submission.getEvaluations()) {
//                if (eval.getExpertAssign() != null
//                        && eval.getExpertAssign().getExpert() != null
//                        && eval.getExpertAssign().getExpert().getExpertId() == expert.getExpertId()) {
//                    evaluation = eval;
//                    break;
//                }
//            }
//        }
//        if (evaluation == null) {
//            throw new BadRequestException("Bạn không phải là giám khảo chấm bài của đội thi này.");
//        }
//
//        if (evaluation.getStatus() != EvaluationStatus.RE_EVALUATION) {
//            throw new BadRequestException("Bài đánh giá này chưa được yêu cầu để chấm lại.");
//        }
//
//        if (evaluation.getOriginalScore() == null) {
//            evaluation.setOriginalScore(evaluation.getScore());
//        }
//        BigDecimal finalNewTotalScore = BigDecimal.ZERO;
//        for (ReDetailEvaluationRequest.EvaluationCriteriaRequest requestEval : request.getCriteriaScores()) {
//
//            EvaluationDetail detail = evaluationDetailRepository.findById(requestEval.getEvaluationDetailId())
//                    .orElseThrow(() -> new BadRequestException("Không tìm thấy tiêu chí chi tiết."));
//
//            if(detail.getEvaluation().getEvaluationId() != evaluation.getEvaluationId()){
//                throw new BadRequestException("Tiêu chí này không thuộc bài chấm đang được phúc khảo.");
//
//            }
//
//            if (detail.getScore().compareTo(BigDecimal.ZERO) < 0 || detail.getScore().compareTo(BigDecimal.valueOf(100)) > 100) {
//                throw new BadRequestException("Điểm của từng tiêu chí không được nhỏ hơn 0 hoặc lớn hơn 100.");
//            }
//            //  lưu điểm cũ của tiêu chí này nếu là lần đầu chấm lại
//            if (detail.getOriginalScore() == null) {
//                detail.setOriginalScore(detail.getScore());
//            }
//
//            detail.setScore(requestEval.getNewScore());
//            evaluationDetailRepository.save(detail);
//            finalNewTotalScore = finalNewTotalScore.add(requestEval.getNewScore());
//
//        }
//
//        evaluation.setComment(request.getComment());
//        evaluation.setScore(finalNewTotalScore);
//        evaluation.setStatus(EvaluationStatus.GRADED);
//        evaluation.setIsReEvaluation(true);
//
//        evaluationRepository.save(evaluation);
//
//    }


}

