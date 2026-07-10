package com.hackathon.service.grading;

import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.evaluation.*;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.RoundRepository;
import com.hackathon.repository.SubmissionRepository;
import com.hackathon.service.grading.support.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Hiện thực dịch vụ chấm điểm. Đóng vai trò là Bộ điều hợp tiến trình (Orchestrator Pattern).
 * Lớp này điều phối các thành phần Support, không chứa logic tính toán trực tiếp.
 */
@Service
@RequiredArgsConstructor
public class GradingServiceImpl implements GradingService {

    private final SubmissionRepository submissionRepository;
    private final EvaluationRepository evaluationRepository;
    private final RoundRepository roundRepository;

    // Tiêm các thành phần xử lý quy tắc nghiệp vụ (SOLID Components)
    private final JudgeAssignmentResolver assignmentResolver;
    private final RoundEndTimeGradingPolicy deadlinePolicy;
    private final CriteriaCompletenessValidator criteriaValidator;
    private final ScoreCalculator scoreCalculator;
    private final EvaluationMapper evaluationMapper;
    private final EvaluationAuditLogger auditLogger;

    // =======================================================
    // API: TRẢ RA DANH SÁCH BÀI CẦN CHẤM
    // =======================================================
    @Override
    public List<AssignedSubmissionForJudgeResponse> listAssignedSubmissions(Account account, Integer categoryRoundId) {
        Expert expert = assignmentResolver.resolveExpert(account);
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(expert, categoryRoundId);

        // Kéo list bài thi final từ DB lên
        List<Submission> submissions = submissionRepository.findFinalSubmissionsByCategoryRoundId(categoryRoundId);

        // Map data để FE hiển thị trạng thái (Đã chấm hay chưa)
        return submissions.stream().map(sub -> {
            Evaluation eval = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), sub.getSubmissionId())
                    .orElse(null);

            return AssignedSubmissionForJudgeResponse.builder()
                    .submissionId(sub.getSubmissionId())
                    .teamName(sub.getTeam().getTeamName())
                    .description(sub.getDescription())
                    .githubUrl(sub.getGithubUrl())
                    .submittedAt(sub.getCreateAt())
                    .myEvaluationStatus(eval != null ? eval.getStatus().name() : "NOT_GRADED")
                    .myTotalScore(eval != null ? eval.getScore() : null)
                    .build();
        }).collect(Collectors.toList());
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
                        .description(c.getDescription())
                        .type(c.getType())
                        .build())
                .collect(Collectors.toList());
    }

    // =========================================================================
    // API: XEM LẠI ĐIỂM CŨ ĐỂ SỬA
    // =========================================================================
    @Override
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

        // 6. Map ra DTO trả về cho Client tái hiện giao diện
        return evaluationMapper.toResponse(evaluation, isEditable, deadline);
    }

    // =======================================================
    // API: UPSERT (LƯU ĐIỂM HOẶC CẬP NHẬT ĐIỂM)
    // =======================================================
    @Override
    @Transactional(rollbackFor = Exception.class) // Đảm bảo tính nguyên tử (Atomicity): Lỗi bất kỳ khâu nào sẽ phục hồi DB nguyên trạng
    public JudgeEvaluationResponse submitOrUpdate(Account account, Integer submissionId, SubmitEvaluationRequest request) {

        // 1. Phân tích ngữ cảnh người dùng: Xác thực đối tượng Chuyên gia
        Expert expert = assignmentResolver.resolveExpert(account);

        // 2. Kiểm tra sự tồn tại của Bài nộp (Submission) trong cơ sở dữ liệu
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu Bài nộp với mã định danh cung cấp: " + submissionId));

        // 3. Ràng buộc nghiệp vụ: Tuyệt đối không cho phép chấm điểm trên các bài nộp là Bản nháp (Draft)
        if (!submission.isFinal()) {
            throw new BadRequestException("Hành động bị từ chối: Bài nộp hiện tại đang ở trạng thái bản nháp, chưa được xác nhận nộp chính thức.");
        }

        // 4. Khai thác dữ liệu quan hệ bắc cầu: Submission -> TeamParticipant -> CategoryRound -> Round
        TeamParticipant participant = submission.getTeamParticipant();
        CategoryRound categoryRound = participant.getCategoryRound();
        Round round = categoryRound.getRound();

        // 5. Kiểm tra phân công chi tiết: Xác định vai trò Judge hợp lệ tại CategoryRound (Hàm này đồng thời ngăn chặn Mentor)
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(expert, categoryRound.getCategoryRoundId());

        // 6. Kiểm tra thời hạn: Từ chối xử lý nếu thời gian hiện tại vượt mốc cấu hình đóng cổng chấm điểm của Round
        if (!deadlinePolicy.isGradingOpen(round)) {
            throw new BadRequestException("Hành động thất bại: Hệ thống đã khóa sổ dữ liệu chấm điểm do quá thời hạn quy định.");
        }

        // 7. Thực hiện thẩm định tính toàn vẹn của danh sách tiêu chí gửi lên
        List<EvaluationCriteria> roundCriteria = round.getEvaluationCriterias();
        criteriaValidator.validate(request, roundCriteria);

        // 8. ÁP DỤNG MÔ HÌNH UPSERT (Update hoặc Insert độc lập)
        boolean isFirstTimeGrading = false;
        Evaluation evaluation = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), submissionId)
                .orElse(null);

        if (evaluation == null) {
            // Trường hợp 1: Chưa từng tồn tại bản ghi đánh giá -> Khởi tạo thực thể mới (Insert)
            isFirstTimeGrading = true;
            evaluation = new Evaluation();
            evaluation.setExpertAssign(expertAssign);
            evaluation.setSubmission(submission);
//            evaluation.setTeamParticipant(participant);
            evaluation.setIsReEvaluation(false);
        } else {
            // Trường hợp 2: Đã tồn tại bản ghi (Update) -> Chặn nếu thực thể đang nằm trong trạng thái xử lý Phúc khảo tách biệt
            if (evaluation.getStatus() != null && "RE_EVALUATION".equals(evaluation.getStatus().name())) {
                throw new BadRequestException("Hành động bị chặn: Thực thể đánh giá đang nằm trong trạng thái Khiếu nại/Phúc khảo hệ thống.");
            }
        }

        // 9. ĐỒNG BỘ HÓA DỮ LIỆU ĐIỂM CHI TIẾT (EvaluationDetail Mapping)
        Map<Integer, EvaluationDetail> existingDetailsMap = evaluation.getEvaluationDetails().stream()
                .collect(Collectors.toMap(d -> d.getEvaluationCriteria().getEvaluationCriteriaId(), d -> d));
        Map<Integer, EvaluationCriteria> criteriaByIdMap = roundCriteria.stream()
                .collect(Collectors.toMap(EvaluationCriteria::getEvaluationCriteriaId, c -> c));

        for (CriteriaScoreRequest scoreReq : request.getCriteriaScores()) {
            EvaluationCriteria criteria = criteriaByIdMap.get(scoreReq.getEvaluationCriteriaId());

            // Tái sử dụng bản ghi chi tiết cũ để cập nhật đè dữ liệu, tránh tạo bản ghi trùng lặp rác dữ liệu
            EvaluationDetail detail = existingDetailsMap.getOrDefault(criteria.getEvaluationCriteriaId(), new EvaluationDetail());
            detail.setEvaluationCriteria(criteria);
            detail.setScore(scoreReq.getScore());
            detail.setOriginalScore(scoreReq.getScore()); // Ghi vết điểm số gốc ban đầu phục vụ lưu vết dữ liệu
            detail.setComment(scoreReq.getComment());
            detail.setEvaluation(evaluation);

            if (detail.getId() == 0) {
                evaluation.getEvaluationDetails().add(detail);
            }
        }

        // 10. TÍNH TOÁN LẠI TỔNG ĐIỂM (Ủy thác quyền cho ScoreCalculator hạ tầng xử lý)
        BigDecimal calculatedTotalScore = scoreCalculator.calculateWeightedTotal(evaluation.getEvaluationDetails());

        evaluation.setScore(calculatedTotalScore);
        evaluation.setOriginalScore(calculatedTotalScore);
        evaluation.setComment(request.getComment());
        evaluation.setStatus(EvaluationStatus.GRADED); // Chuyển dịch trạng thái thực thể sang Đã chấm điểm

        // 11. ĐẨY DỮ LIỆU XUỐNG DB & KÍCH HOẠT LƯU VẾT HỆ THỐNG (Audit Service Log)
        evaluation = evaluationRepository.save(evaluation);
        auditLogger.logGraded(account, evaluation, submission, expert.getExpertId(), isFirstTimeGrading, calculatedTotalScore);

        LocalDateTime deadline = deadlinePolicy.getGradingDeadline(round);
        // 12. CHUYỂN ĐỔI DỮ LIỆU ĐẦU RA VÀ PHẢN HỒI PRESENTATION TẦNG
        return evaluationMapper.toResponse(evaluation, true, deadline); // Khẳng định cờ isEditable = true vì đang nằm trong khung hạn cho phép sửa
    }
}