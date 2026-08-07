package com.hackathon.service.impl;

import com.hackathon.dto.evaluation.*;
import com.hackathon.dto.submission.FileDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.service.grading.GradingService;
import com.hackathon.service.grading.support.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Hiện thực dịch vụ chấm điểm. Đóng vai trò là Bộ điều hợp tiến trình (Orchestrator Pattern).
// Lớp này điều phối các thành phần Support, không chứa logic tính toán trực tiếp.
@Service
@RequiredArgsConstructor
// Điều phối toàn bộ quy trình lấy bài chấm, lưu điểm và xử lý chấm lại của giám khảo.
public class GradingServiceImpl implements GradingService {

    private final SubmissionRepository submissionRepository;
    private final EvaluationRepository evaluationRepository;
    private final RoundRepository roundRepository;
    private final TeamRequestRepository teamRequestRepository;

    private final JudgeAssignmentResolver assignmentResolver;
    private final RoundEndTimeGradingPolicy deadlinePolicy;
    private final CriteriaCompletenessValidator criteriaValidator;
    private final ScoreCalculator scoreCalculator;
    private final EvaluationMapper evaluationMapper;
    private final EvaluationAuditLogServiceImpl evaluationAuditLogServiceImpl;
    private final CategoryRoundRepository categoryRoundRepository;


    // =======================================================
    // API: TRẢ RA DANH SÁCH BÀI CẦN CHẤM
    // =======================================================
    @Override
    public JudgeDashboardResponse listAssignedSubmissions(
            Account account, Integer categoryRoundId) {
        Expert expert = assignmentResolver.resolveExpert(account);
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(expert, categoryRoundId);

        // 1. Lấy thông tin Vòng thi để tính toán Deadline
        CategoryRound categoryRound = categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạng mục thuộc vòng đấu"));
        Round round = categoryRound.getRound();

        // Lấy thông tin thời gian từ Policy
        LocalDateTime deadline = deadlinePolicy.getGradingDeadline(round);
        boolean isOpen = deadlinePolicy.isGradingOpen(round);

        // 2. Kéo list bài thi final từ DB lên (giữ nguyên logic cũ)
        List<Submission> submissions = submissionRepository.findFinalSubmissionsByCategoryRoundId(categoryRoundId);

        // 3. Map data bài thi
        List<AssignedSubmissionForJudgeResponse> submissionResponses = submissions.stream().map(sub -> {
            Evaluation eval = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), sub.getSubmissionId())
                    .orElse(null);

            // Bài đang chấm lại chỉ xuất hiện ở API re-evaluations.
            if (eval != null && eval.getStatus() == EvaluationStatus.RE_EVALUATION) {
                return null;
            }

            List<FileDTO> fileDTOList = new ArrayList<>();
            if (sub.getFiles() != null) {
                for (com.hackathon.entity.SubmissionFile f : sub.getFiles()) {
                    fileDTOList.add(new FileDTO(f.getFileName(), f.getFileUrl()));
                }
            }
            String commitUrl = sub.getGithubUrl();
            if (commitUrl != null && sub.getLatestCommitSha() != null
                    && !sub.getLatestCommitSha().isBlank()) {
                commitUrl += "/commit/" + sub.getLatestCommitSha();
            }

            return AssignedSubmissionForJudgeResponse.builder()
                    .submissionId(sub.getSubmissionId())
                    .teamName(sub.getTeam().getTeamName())
                    .description(sub.getDescription())
                    .githubUrl(commitUrl)
                    .files(fileDTOList)
                    .submittedAt(sub.getCreateAt())
                    .myEvaluationStatus(eval != null ? eval.getStatus().name() : "NOT_GRADED")
                    .myTotalScore(eval != null ? eval.getScore() : null)
                    .build();
        }).filter(java.util.Objects::nonNull).collect(Collectors.toList());

        return JudgeDashboardResponse.builder()
                .gradingDeadline(deadline)
                .isGradingOpen(isOpen)
                .submissions(submissionResponses)
                .build();
    }

    // =======================================================
    // API: TRẢ RA DANH SÁCH BÀI CẦN CHẤM LẠI
    // =======================================================
    @Override
    public JudgeDashboardResponse listReEvaluationSubmissions(
            Account account, Integer categoryRoundId) {
        Expert expert = assignmentResolver.resolveExpert(account);
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(
                expert, categoryRoundId);

        CategoryRound categoryRound = categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hạng mục thuộc vòng đấu"));
        Round round = categoryRound.getRound();

        LocalDateTime deadline = round.getResolveAppealDeadline();
        boolean isOpen = deadline != null && LocalDateTime.now().isBefore(deadline);

        List<Submission> submissions = submissionRepository
                .findFinalSubmissionsByCategoryRoundId(categoryRoundId);

        List<AssignedSubmissionForJudgeResponse> submissionResponses = submissions.stream()
                .map(submission -> {
                    Evaluation evaluation = evaluationRepository
                            .findByExpertAssignIdAndSubmissionId(
                                    expertAssign.getAssignId(),
                                    submission.getSubmissionId())
                            .orElse(null);

                    if (evaluation == null
                            || evaluation.getStatus() != EvaluationStatus.RE_EVALUATION) {
                        return null;
                    }

                    List<FileDTO> files = new ArrayList<>();
                    if (submission.getFiles() != null) {
                        for (SubmissionFile file : submission.getFiles()) {
                            files.add(new FileDTO(
                                    file.getFileName(), file.getFileUrl()));
                        }
                    }

                    String commitUrl = submission.getGithubUrl();
                    if (commitUrl != null && submission.getLatestCommitSha() != null
                            && !submission.getLatestCommitSha().isBlank()) {
                        commitUrl += "/commit/" + submission.getLatestCommitSha();
                    }

                    return AssignedSubmissionForJudgeResponse.builder()
                            .submissionId(submission.getSubmissionId())
                            .teamName(submission.getTeam().getTeamName())
                            .description(submission.getDescription())
                            .githubUrl(commitUrl)
                            .files(files)
                            .submittedAt(submission.getCreateAt())
                            .myEvaluationStatus(evaluation.getStatus().name())
                            .myTotalScore(evaluation.getScore())
                            .build();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        return JudgeDashboardResponse.builder()
                .gradingDeadline(deadline)
                .isGradingOpen(isOpen)
                .submissions(submissionResponses)
                .build();
    }

    // =======================================================
    // API: LẤY FORM TIÊU CHÍ
    // =======================================================
    @Override
    public List<EvaluationCriteriaResponse> viewScoringCriteria(Integer roundId) {
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResourceNotFoundException("Vòng thi không tồn tại!"));

        return round.getEvaluationCriterias().stream()
                .map(c -> EvaluationCriteriaResponse.builder()
                        .evaluationCriteriaId(c.getEvaluationCriteriaId())
                        .criteriaName(c.getCriteriaName())
                        .weight(c.getWeight())
                        .maxScore(c.getMaxScore())
                        .description(c.getDescription())
                        .type(c.getType())
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================================
    // API: XEM LẠI ĐIỂM CŨ ĐỂ SỬA
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public JudgeEvaluationResponse viewMyEvaluation(Account account, Integer submissionId) {

        // 1. Phân tích ngữ cảnh bảo mật: Xác thực Chuyên gia và Bài nộp
        Expert expert = assignmentResolver.resolveExpert(account);
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu Bài nộp: " + submissionId));

        Round round = submission.getTeamParticipant().getCategoryRound().getRound();

        // 2. Xác minh quyền: Đảm bảo ông này là Judge của đúng Vòng thi đó
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(
                expert, submission.getTeamParticipant().getCategoryRound().getCategoryRoundId());

        // 3. Kéo bản ghi điểm số cũ lên
        Evaluation evaluation = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Giám khảo chưa từng chấm bài này. Vui lòng sử dụng luồng Chấm mới!"));

        // 4. Kiểm tra xem thời gian hiện tại còn cho phép sửa điểm không?
        // Nếu đã qua Deadline, cờ isEditable sẽ = false, Frontend dựa vào cờ này để disable (làm mờ) nút Lưu.
        boolean isEditable = deadlinePolicy.isGradingOpen(round);

        // 5. Lấy thông tin thời gian Deadline cấu hình
        LocalDateTime deadline = deadlinePolicy.getGradingDeadline(round);

        // Kéo danh sách hội đồng và truyền vào Mapper
        List<Evaluation> otherEvaluations = getOtherEvaluations(submissionId, expertAssign.getAssignId());

        return evaluationMapper.toResponse(evaluation, isEditable, deadline, otherEvaluations);
    }


    // =======================================================
    // API 4.1 & 4.2: CHẤM ĐIỂM TỪNG PHẦN (PARTIAL UPSERT)
    // =======================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    // Đảm bảo tính nguyên tử (Atomicity): Lỗi bất kỳ khâu nào sẽ phục hồi DB nguyên trạng
    public JudgeEvaluationResponse submitPartialEvaluation(Account account, Integer submissionId, SubmitEvaluationRequest request, CriteriaType targetType) {

        // 1. Phân tích ngữ cảnh người dùng: Xác thực đối tượng Chuyên gia
        Expert expert = assignmentResolver.resolveExpert(account);

        // 2. Kiểm tra sự tồn tại của Bài nộp (Submission) trong cơ sở dữ liệu
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu Bài nộp với mã định danh cung cấp: " + submissionId));

        // 3. Ràng buộc nghiệp vụ: Tuyệt đối không cho phép chấm điểm trên các bài nộp là Bản nháp (Draft)
        if (!submission.isFinal()) {
            throw new BadRequestException("Bài nộp hiện tại đang ở trạng thái bản nháp, chưa được xác nhận nộp chính thức.");
        }

        // 4. Khai thác dữ liệu quan hệ bắc cầu: Submission -> TeamParticipant -> CategoryRound -> Round
        TeamParticipant participant = submission.getTeamParticipant();
        CategoryRound categoryRound = participant.getCategoryRound();
        Round round = categoryRound.getRound();

        // 5. Kiểm tra phân công chi tiết: Xác định vai trò Judge hợp lệ tại CategoryRound
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(expert, categoryRound.getCategoryRoundId());

        // 6. Thực hiện thẩm định tính toàn vẹn (Chỉ thẩm định các tiêu chí thuộc phần targetType đang chấm)
        List<EvaluationCriteria> roundCriteria = round.getEvaluationCriterias();

        // Gọi hàm validatePartial mà chúng ta đã định nghĩa ở Validator
        criteriaValidator.validatePartial(request, roundCriteria, targetType);

        // 8. ÁP DỤNG MÔ HÌNH UPSERT VÀ CHỤP NHANH DỮ LIỆU CŨ (SNAPSHOT)
        Evaluation evaluation = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), submissionId)
                .orElse(null);

        EvaluationStatus previousStatus = evaluation != null
                ? evaluation.getStatus()
                : EvaluationStatus.NOT_GRADED;
        boolean isReEvaluation = previousStatus == EvaluationStatus.RE_EVALUATION;

        if (evaluation == null) {
            // Trường hợp 1: Chưa từng tồn tại bản ghi đánh giá -> Khởi tạo thực thể mới (Insert)
            evaluation = new Evaluation();
            evaluation.setExpertAssign(expertAssign);
            evaluation.setSubmission(submission);
            evaluation.setIsReEvaluation(false);
            evaluation.setEvaluationDetails(new ArrayList<>());
        }

        deadlinePolicy.validateScoringTime(round, isReEvaluation);

        // Lấy danh sách điểm cũ chuyển thành Map để thao tác Add/Update trực tiếp trên từng Item,
        // giúp bảo toàn các điểm đã chấm ở phần khác (Ví dụ đang chấm CODE thì giữ nguyên điểm PRESENTATION)
        Map<Integer, EvaluationDetail> existingDetailsMap = evaluation.getEvaluationDetails().stream()
                .collect(Collectors.toMap(d -> d.getEvaluationCriteria().getEvaluationCriteriaId(), d -> d));

        Map<Integer, EvaluationCriteria> targetCriteriaMap = roundCriteria.stream()
                .filter(c -> c.getType() == targetType)
                .collect(Collectors.toMap(EvaluationCriteria::getEvaluationCriteriaId, c -> c));

        for (CriteriaScoreRequest scoreReq : request.getCriteriaScores()) {
            EvaluationCriteria criteria = targetCriteriaMap.get(scoreReq.getEvaluationCriteriaId());
            if (criteria == null) continue;

            // Tái sử dụng bản ghi chi tiết cũ để cập nhật đè (Update), hoặc tạo mới (Add) nếu chưa có
            EvaluationDetail detail = existingDetailsMap.getOrDefault(criteria.getEvaluationCriteriaId(), new EvaluationDetail());

            detail.setEvaluationCriteria(criteria);
            detail.setScore(scoreReq.getScore());
            detail.setComment(scoreReq.getComment());
            detail.setEvaluation(evaluation);
            if (isReEvaluation) {
                detail.setIsReEvaluation(true);
            }

            if (detail.getId() == 0) {
                evaluation.getEvaluationDetails().add(detail);
            }
        }

        // 10. TÍNH TOÁN LẠI TỔNG ĐIỂM (Ủy thác quyền cho ScoreCalculator tính toán dựa trên list đã Merge)
        BigDecimal calculatedTotalScore = scoreCalculator.calculateWeightedTotal(evaluation.getEvaluationDetails());
        evaluation.setScore(calculatedTotalScore);
        evaluation.setComment(request.getComment());
        evaluation.setStatus(determineNextStatus(evaluation, roundCriteria, isReEvaluation));
        if (isReEvaluation && evaluation.getStatus() == EvaluationStatus.GRADED) {
            evaluation.setIsReEvaluation(false);
        }

        // 11. ĐẨY DỮ LIỆU XUỐNG DB & KÍCH HOẠT LƯU VẾT HỆ THỐNG (Audit Service Log)
        evaluation = evaluationRepository.save(evaluation);

        evaluationAuditLogServiceImpl.saveAttempt(
                account,
                evaluation,
                targetType,
                isReEvaluation);

        if (isReEvaluation && evaluation.getStatus() == EvaluationStatus.GRADED) {
            updateAppealProgress(evaluation, expert.getExpertName());
        }

        LocalDateTime deadline = isReEvaluation
                ? round.getResolveAppealDeadline()
                : deadlinePolicy.getGradingDeadline(round);

        // Kéo danh sách hội đồng và truyền vào Mapper
        List<Evaluation> otherEvaluations = getOtherEvaluations(submissionId, expertAssign.getAssignId());

        return evaluationMapper.toResponse(evaluation, true, deadline, otherEvaluations);
    }

    // Xác định trạng thái tiếp theo dựa trên mức độ hoàn thành các tiêu chí bắt buộc.
    private EvaluationStatus determineNextStatus(
            Evaluation evaluation,
            List<EvaluationCriteria> requiredCriteria,
            boolean isReEvaluation) {
        // Khi chấm lại, chỉ tính các chi tiết đã được đánh dấu thuộc lần chấm lại hiện tại.
        if (isReEvaluation) {
            // Tạo tập mã tiêu chí bắt buộc để so sánh không phụ thuộc thứ tự.
            Set<Integer> requiredIds = requiredCriteria.stream()
                    .map(EvaluationCriteria::getEvaluationCriteriaId)
                    .collect(Collectors.toSet());
            // Tạo tập mã tiêu chí đã hoàn thành chấm lại.
            Set<Integer> reEvaluatedIds = evaluation.getEvaluationDetails().stream()
                    .filter(detail -> detail.getEvaluationCriteria() != null)
                    .filter(detail -> Boolean.TRUE.equals(detail.getIsReEvaluation()))
                    .map(detail -> detail.getEvaluationCriteria()
                            .getEvaluationCriteriaId())
                    .collect(Collectors.toSet());
            // Chỉ hoàn tất khi tập tiêu chí đã chấm lại chứa toàn bộ tiêu chí bắt buộc.
            boolean allReEvaluated = reEvaluatedIds.containsAll(requiredIds);
            return allReEvaluated
                    ? EvaluationStatus.GRADED
                    : EvaluationStatus.RE_EVALUATION;
        }

        // Với lần chấm thông thường, lấy tập mã của toàn bộ tiêu chí bắt buộc.
        Set<Integer> requiredIds = requiredCriteria.stream()
                .map(EvaluationCriteria::getEvaluationCriteriaId)
                .collect(Collectors.toSet());
        // Chỉ tính tiêu chí có điểm và còn liên kết hợp lệ với cấu hình vòng.
        Set<Integer> gradedIds = evaluation.getEvaluationDetails().stream()
                .filter(detail -> detail.getScore() != null)
                .filter(detail -> detail.getEvaluationCriteria() != null)
                .map(detail -> detail.getEvaluationCriteria().getEvaluationCriteriaId())
                .collect(Collectors.toSet());

        // Đủ tất cả tiêu chí thì hoàn tất, nếu thiếu thì giữ trạng thái chấm một phần.
        return gradedIds.containsAll(requiredIds)
                ? EvaluationStatus.GRADED
                : EvaluationStatus.PARTIALLY_GRADED;
    }

    // Cập nhật tiến độ đơn khiếu nại sau khi một giám khảo hoàn thành chấm lại.
    private void updateAppealProgress(Evaluation completedEvaluation, String expertName) {
        // Lấy bài nộp và đội tham gia để xác định đúng đơn khiếu nại liên quan.
        Submission submission = completedEvaluation.getSubmission();
        TeamParticipant participant = submission.getTeamParticipant();
        Integer submissionId = submission.getSubmissionId();
        Integer teamId = submission.getTeam().getTeamId();
        Integer roundId = participant.getCategoryRound().getRound().getRoundId();

        // Kiểm tra còn giám khảo nào của cùng bài vẫn đang chờ chấm lại hay không.
        boolean stillWaiting = evaluationRepository
                .existsBySubmission_SubmissionIdAndStatus(
                        submissionId, EvaluationStatus.RE_EVALUATION);

        // Tìm đơn khiếu nại đang xử lý của đúng đội và vòng thi.
        TeamRequest appealRequest = teamRequestRepository
                .findByTeam_TeamIdAndRound_RoundIdAndRequestTypeAndStatus(
                        teamId,
                        roundId,
                        RequestType.APPEAL,
                        RequestStatus.PROCESSING)
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy đơn khiếu nại đang được xử lý cho bài chấm này."));

        // Nếu còn người chưa hoàn tất, giữ đơn ở trạng thái đang xử lý và cập nhật tiến độ.
        if (stillWaiting) {
            appealRequest.setStatus(RequestStatus.PROCESSING);
            appealRequest.setResponseMessage(String.format(
                    "Giám khảo %s đã cập nhật điểm. Đang chờ các giám khảo khác hoàn tất.",
                    expertName));
        } else {
            appealRequest.setStatus(RequestStatus.IN_REVIEW);
            appealRequest.setResponseMessage(
                    "Toàn bộ hội đồng giám khảo đã hoàn tất cập nhật điểm phúc khảo.");
        }

        // Ghi thời điểm cập nhật gần nhất và lưu trạng thái mới của đơn.
        appealRequest.setResponseAt(LocalDateTime.now());
        teamRequestRepository.save(appealRequest);
    }

    // Hàm Helper: Lấy danh sách bài chấm của Hội đồng (Trừ bản thân)
    private List<Evaluation> getOtherEvaluations(Integer submissionId, Integer currentAssignId) {
        List<EvaluationStatus> validStatuses = List.of(EvaluationStatus.GRADED, EvaluationStatus.RE_EVALUATION);
        return evaluationRepository.findOtherBoardEvaluations(submissionId, currentAssignId, validStatuses);
    }

}
