package com.hackathon.service.grading;

import com.hackathon.dto.evaluation.*;
import com.hackathon.dto.submission.FileDTO;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.*;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.*;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AuditService;
import com.hackathon.service.grading.support.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final ExpertRepository expertRepository;
    private final TeamRequestRepository teamRequestRepository;

    // Tiêm các thành phần xử lý quy tắc nghiệp vụ (SOLID Components)
    private final JudgeAssignmentResolver assignmentResolver;
    private final RoundEndTimeGradingPolicy deadlinePolicy;
    private final CriteriaCompletenessValidator criteriaValidator;
    private final ScoreCalculator scoreCalculator;
    private final EvaluationMapper evaluationMapper;
    private final EvaluationAuditLogger auditLogger;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;


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

            List<FileDTO> fileDTOList = new ArrayList<>();
            if (sub.getFiles() != null) {
                for (com.hackathon.entity.SubmissionFile f : sub.getFiles()) {
                    fileDTOList.add(new FileDTO(f.getFileName(), f.getFileUrl()));
                }
            }
            String commitUrl = sub.getGithubUrl() + "/commit/" + sub.getLatestCommitSha();
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
    // API 4.1 & 4.2: CHẤM ĐIỂM TỪNG PHẦN (PARTIAL UPSERT)
    // =======================================================
    @Override
    @Transactional(rollbackFor = Exception.class) // Đảm bảo tính nguyên tử (Atomicity): Lỗi bất kỳ khâu nào sẽ phục hồi DB nguyên trạng
    public JudgeEvaluationResponse submitPartialEvaluation(Account account, Integer submissionId, SubmitEvaluationRequest request, CriteriaType targetType) {

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

        // 5. Kiểm tra phân công chi tiết: Xác định vai trò Judge hợp lệ tại CategoryRound
        ExpertAssign expertAssign = assignmentResolver.requireJudgeAssignment(expert, categoryRound.getCategoryRoundId());

        // 6. Kiểm tra thời hạn: Từ chối xử lý nếu thời gian hiện tại vượt mốc cấu hình đóng cổng chấm điểm của Round
        if (!deadlinePolicy.isGradingOpen(round)) {
            throw new BadRequestException("Hành động thất bại: Hệ thống đã khóa sổ dữ liệu chấm điểm do quá thời hạn quy định.");
        }

        // 7. Thực hiện thẩm định tính toàn vẹn (Chỉ thẩm định các tiêu chí thuộc phần targetType đang chấm)
        List<EvaluationCriteria> roundCriteria = round.getEvaluationCriterias();
        BigDecimal maxScale = BigDecimal.valueOf(10);

        if (round.getCriteriaSet() != null && round.getCriteriaSet().getMaxScore() != null) {
            maxScale = BigDecimal.valueOf(round.getCriteriaSet().getMaxScore());
        }

        // Gọi hàm validatePartial mà chúng ta đã định nghĩa ở Validator
        criteriaValidator.validatePartial(request, roundCriteria, maxScale, targetType);

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
            evaluation.setIsReEvaluation(false);
            evaluation.setEvaluationDetails(new ArrayList<>());
        } else {
            // Trường hợp 2: Đã tồn tại bản ghi (Update) -> Chặn nếu thực thể đang nằm trong trạng thái xử lý Phúc khảo
            if (evaluation.getStatus() != null && "RE_EVALUATION".equals(evaluation.getStatus().name())) {
                throw new BadRequestException("Hành động bị chặn: Thực thể đánh giá đang nằm trong trạng thái Khiếu nại/Phúc khảo hệ thống.");
            }
        }

        // 9. ĐỒNG BỘ HÓA DỮ LIỆU ĐIỂM CHI TIẾT (PARTIAL MAPPING & MERGE)
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

            if (detail.getId() == 0) {
                evaluation.getEvaluationDetails().add(detail);
            }
        }

        // 10. TÍNH TOÁN LẠI TỔNG ĐIỂM (Ủy thác quyền cho ScoreCalculator tính toán dựa trên list đã Merge)
        BigDecimal calculatedTotalScore = scoreCalculator.calculateWeightedTotal(evaluation.getEvaluationDetails());

        evaluation.setScore(calculatedTotalScore);
        // evaluation.setOriginalScore(calculatedTotalScore);
        evaluation.setComment(request.getComment());
        evaluation.setStatus(EvaluationStatus.GRADED); // Chuyển dịch trạng thái thực thể sang Đã chấm điểm

        // 11. ĐẨY DỮ LIỆU XUỐNG DB & KÍCH HOẠT LƯU VẾT HỆ THỐNG (Audit Service Log)
        evaluation = evaluationRepository.save(evaluation);
        auditLogger.logGraded(account, evaluation, submission, expert.getExpertId(), isFirstTimeGrading, calculatedTotalScore);

        LocalDateTime deadline = deadlinePolicy.getGradingDeadline(round);

        // 12. CHUYỂN ĐỔI DỮ LIỆU ĐẦU RA VÀ PHẢN HỒI PRESENTATION TẦNG
        return evaluationMapper.toResponse(evaluation, true, deadline);
    }

    // Chấm điểm lại khi bị event coordinator từ chối
    @Override
    @Transactional
    public JudgeEvaluationResponse updateEvaluation(Account account, Integer submissionId, SubmitEvaluationRequest request
    ,CriteriaType targetType) {
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

        // 6. Chỉ update những dữ liệu ở trạng thái RE_EVALUATION khi bị event từ chối yêu cầu chấm điểm lại
        Evaluation evaluation = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), submissionId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy dữ liệu chấm điểm"));

        if (evaluation.getStatus() != EvaluationStatus.RE_EVALUATION) {
            throw new BadRequestException("Hành động thất bại: Chỉ có thể cập nhật điểm số khi BTC yêu cầu chấm lại");
        }
        // 7. Thực hiện thẩm định tính toàn vẹn của danh sách tiêu chí gửi lên
        List<EvaluationCriteria> roundCriteria = round.getEvaluationCriterias();
        BigDecimal maxScale = BigDecimal.valueOf(10);

        if (round.getCriteriaSet() != null && round.getCriteriaSet().getMaxScore() != null) {
            maxScale = BigDecimal.valueOf(round.getCriteriaSet().getMaxScore());
        }

        criteriaValidator.validate(request, roundCriteria, maxScale);

        // 8. Cập nhật lại điểm số
        evaluation.setExpertAssign(expertAssign);
        evaluation.setSubmission(submission);

        // 9. ĐỒNG BỘ HÓA DỮ LIỆU ĐIỂM CHI TIẾT (EvaluationDetail Mapping)

        Map<Integer, EvaluationDetail> existingDetailsMap = evaluation.getEvaluationDetails().stream()
                .collect(Collectors.toMap(d -> d.getEvaluationCriteria().getEvaluationCriteriaId(), d -> d));
        Map<Integer, EvaluationCriteria> criteriaByIdMap = roundCriteria.stream()
                .collect(Collectors.toMap(EvaluationCriteria::getEvaluationCriteriaId, c -> c));

        for (CriteriaScoreRequest scoreReq : request.getCriteriaScores()) {
            EvaluationCriteria criteria = criteriaByIdMap.get(scoreReq.getEvaluationCriteriaId());
            if (criteria == null) continue;

//            CriteriaSet criteriaSet = round.getCriteriaSet();
//            BigDecimal maxScore = BigDecimal.valueOf(criteriaSet.getMaxScore());
//
//            if (scoreReq.getScore().compareTo(BigDecimal.ZERO) < 0 || scoreReq.getScore().compareTo(maxScore) > 0) {
//                throw new BadRequestException(
//                        "Điểm của tiêu chí " + "phải nằm trong khoảng từ 0 đến " + maxScore + ".");
//            }


            // Tái sử dụng bản ghi chi tiết cũ để cập nhật đè dữ liệu, tránh tạo bản ghi trùng lặp rác dữ liệu
            EvaluationDetail detail = existingDetailsMap.get(criteria.getEvaluationCriteriaId());
            boolean isNewDetail = false;

            if (detail == null) {
                detail = new EvaluationDetail();
                isNewDetail = true;
            }


            // kiểm tra ID để tránh bỏ sót phần tử mới
            if (isNewDetail || detail.getId() == 0) {
                evaluation.getEvaluationDetails().add(detail);
            }

            detail.setEvaluationCriteria(criteria);
            detail.setScore(scoreReq.getScore());
            detail.setComment(scoreReq.getComment());
            detail.setEvaluation(evaluation);
        }

        // 10. TÍNH TOÁN LẠI TỔNG ĐIỂM (Ủy thác quyền cho ScoreCalculator hạ tầng xử lý)
        BigDecimal calculatedTotalScore = scoreCalculator.calculateWeightedTotal(evaluation.getEvaluationDetails());

        evaluation.setScore(calculatedTotalScore);
        evaluation.setComment(request.getComment());
        evaluation.setStatus(EvaluationStatus.GRADED);

        // 11. ĐẨY DỮ LIỆU XUỐNG DB & KÍCH HOẠT LƯU VẾT HỆ THỐNG (Audit Service Log)
        evaluation = evaluationRepository.save(evaluation);
        String description = String.format("Giám khảo (ExpertID: %d) đã cập nhật điểm cho Bài nộp (SubmissionID: %d). Tổng điểm ghi nhận: %s",
                expertAssign.getAssignId(), submission.getSubmissionId(), calculatedTotalScore);

        Map<String, Object> auditData = new LinkedHashMap<>();

        auditData.put("oldTotalScore", evaluation.getOriginalScore());
        auditData.put("newTotalScore", calculatedTotalScore);
        auditData.put("details",
                evaluation.getEvaluationDetails().stream()
                        .map(detail -> Map.of(
                                "criteriaId", detail.getEvaluationCriteria().getEvaluationCriteriaId(),
                                "criteriaName", detail.getEvaluationCriteria().getCriteriaName(),
                                "oldScore", detail.getOriginalScore() != null ? detail.getOriginalScore() : detail.getScore(),
                                "newScore", detail.getScore()
                        ))
                        .toList());

        String data = objectMapper.writeValueAsString(auditData);
        auditService.saveLog(
                account,
                AuditAction.UPDATE_EVALUATION,
                AuditEntityType.EVALUATION,
                evaluation.getEvaluationId(),
                description
        );

        // 12. CHUYỂN ĐỔI DỮ LIỆU ĐẦU RA VÀ PHẢN HỒI PRESENTATION TẦNG
        return evaluationMapper.toResponse(evaluation, false, null);

    }

    // Thực hiện chấm điểm lại khi nhận được yêu cầu của Expert
    @Override
    @Transactional
    public JudgeEvaluationResponse reEvaluationSubmission(CustomUserDetails userDetails, ReEvaluationRequest
            request,CriteriaType targetType) {
        Account account = userDetails.getAccount();
        Expert expert = expertRepository.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException("Tài khoản này không phải là tài khoản của Expert, vì vậy bạn không được phép truy cập vào trình duyệt này."));

        //  Lấy tất cả đơn khiếu nại kết quả của vòng đấu này đang ở trạng thái INREVIEW
        TeamRequest appealRequest = teamRequestRepository.findById(request.getRequestId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn khiếu nại phúc khảo nào."));
        if (appealRequest.getStatus() != RequestStatus.IN_REVIEW) {
            throw new BadRequestException("Đơn khiếu nại này không ở trạng thái INREVIEW");
        }


        // Từ ds khiếu nại lấy ra bài nộp để tiến hành chấm điểm lại
        Round round = appealRequest.getRound();

        TeamParticipant participant = appealRequest.getTeam().getRegistrations().stream()
                .filter(registration -> registration.getStatus() == RegistrationStatus.APPROVED)
                .map(Registration::getParticipants)
                .flatMap(List::stream)
                .filter(tp -> tp.getCategoryRound() != null && tp.getCategoryRound().getRound().getRoundId().equals(round.getRoundId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thông tin tham gia vòng đấu của đội này."));

        CategoryRound categoryRound = participant.getCategoryRound();
        ExpertAssign expertAssign =
                assignmentResolver.requireJudgeAssignment(expert, categoryRound.getCategoryRoundId());

        Submission finalSubmission = participant.getSubmissions().stream()
                .filter(Submission::isFinal)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Đội thi chưa xác nhận nộp bài chính thức cho vòng này."));

        // Từ  bài nộp tìm ra expert này chấm
        Evaluation evaluation = evaluationRepository.findByExpertAssignIdAndSubmissionId(expertAssign.getAssignId(), finalSubmission.getSubmissionId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy dữ liệu chấm điểm cũ của bạn cho đội thi này."));


        if (evaluation.getStatus() != EvaluationStatus.RE_EVALUATION) {
            throw new BadRequestException("Bài đánh giá này chưa được yêu cầu để chấm lại.");
        }

        if (evaluation.getOriginalScore() == null) {
            evaluation.setOriginalScore(evaluation.getScore());
        }
        CriteriaSet criteriaSet = round.getCriteriaSet();

        Map<Integer, EvaluationDetail> detailsMap = evaluation.getEvaluationDetails().stream()
                .filter(d -> d.getEvaluationCriteria() != null)
                .collect(Collectors.toMap(d -> d.getEvaluationCriteria().getEvaluationCriteriaId(), d -> d));

        for (CriteriaScoreRequest requestEval : request.getCriteriaScores()) {
            EvaluationDetail detail = detailsMap.get(requestEval.getEvaluationCriteriaId());
            if (detail == null) {
                throw new BadRequestException("Tiêu chí này không nằm trong danh sách tiêu chí chấm điểm của vòng đấu này.");
            }

            if (detail.getEvaluation().getEvaluationId() != evaluation.getEvaluationId()) {
                throw new BadRequestException("Tiêu chí này không thuộc bài chấm đang được phúc khảo.");

            }
            BigDecimal maxScore = BigDecimal.valueOf(criteriaSet.getMaxScore());
            if (requestEval.getScore().compareTo(BigDecimal.ZERO) < 0 || requestEval.getScore().compareTo(maxScore) > 0) {
                throw new BadRequestException(
                        "Điểm của tiêu chí " + "phải nằm trong khoảng từ 0 đến " + maxScore + ".");
            }
            //  lưu điểm cũ của tiêu chí này nếu là lần đầu chấm lại
            if (detail.getOriginalScore() == null) {
                detail.setOriginalScore(detail.getScore());
            }

            detail.setScore(requestEval.getScore());

        }
        BigDecimal finalNewTotalScore = scoreCalculator.calculateWeightedTotal(evaluation.getEvaluationDetails());

        evaluation.setComment(request.getComment());
        evaluation.setScore(finalNewTotalScore);
        evaluation.setStatus(EvaluationStatus.GRADED);
        evaluation.setIsReEvaluation(true);

        evaluationRepository.save(evaluation);
        // 1. Lấy tất cả các bảng điểm (Evaluation) của bài nộp này từ các giám khảo khác nhau
        List<Evaluation> allEvaluationsForSub = evaluationRepository.findBySubmission_SubmissionId(finalSubmission.getSubmissionId());

        // 2. Kiểm tra xem có ông giám khảo nào còn đang bị kẹt ở trạng thái "RE_EVALUATION" hay không
        boolean isAllJudgesFinished = allEvaluationsForSub.stream()
                .noneMatch(eval -> eval.getStatus() == EvaluationStatus.RE_EVALUATION);

        if (isAllJudgesFinished) {
            // Nếu tất cả bgk đã sửa điểm xong. Đóng đơn khiếu nại hoàn toàn
            appealRequest.setStatus(RequestStatus.RE_EVALUATED);
            round.setStatus(RoundStatus.PENDING_APPROVAL);
            appealRequest.setResponseMessage("Toàn bộ hội đồng Giám khảo đã hoàn tất cập nhật lại điểm số phúc khảo.");
        } else {
            // Nếu vẫn còn giám khảo chưa chấm lại giữ nguyên IN_REVIEW
            appealRequest.setStatus(RequestStatus.IN_REVIEW);
            appealRequest.setResponseMessage(String.format("Giám khảo %s đã sửa điểm. Đang đợi các giám khảo khác trong hội đồng hoàn tất.", expert.getExpertName()));
        }

        appealRequest.setResponseAt(LocalDateTime.now());
        teamRequestRepository.save(appealRequest);
        roundRepository.save(round);

        // ghi log
        Map<String, Object> auditData = new LinkedHashMap<>();

        auditData.put("oldTotalScore", evaluation.getOriginalScore());
        auditData.put("newTotalScore", finalNewTotalScore);
        auditData.put("details",
                evaluation.getEvaluationDetails().stream()
                        .map(detail -> Map.of(
                                "criteriaId", detail.getEvaluationCriteria().getEvaluationCriteriaId(),
                                "criteriaName", detail.getEvaluationCriteria().getCriteriaName(),
                                "oldScore", detail.getOriginalScore() != null ? detail.getOriginalScore() : detail.getScore(),
                                "newScore", detail.getScore()
                        ))
                        .toList());

        String data = objectMapper.writeValueAsString(auditData);
        auditService.saveLog(
                account,
                AuditAction.RE_SUBMIT_EVALUATION,
                AuditEntityType.EVALUATION,
                evaluation.getEvaluationId(),
                "Ban giám khảo chấm lại điểm khi có yêu cầu phúc khảo thành công",
                data);

        return evaluationMapper.toResponse(evaluation, false, null);

    }

}