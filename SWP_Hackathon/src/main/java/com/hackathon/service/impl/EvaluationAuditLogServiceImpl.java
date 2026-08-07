package com.hackathon.service.impl;

import com.hackathon.dto.evaluation.EvaluationAuditAttemptResponse;
import com.hackathon.dto.evaluation.EvaluationDetailAuditResponse;
import com.hackathon.dto.evaluation.EvaluationAuditListResponse;
import com.hackathon.entity.*;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.entity.enums.CriteriaType;
import com.hackathon.repository.EvaluationAuditLogRepository;
import com.hackathon.repository.EventCoordinatorRepository;
import com.hackathon.repository.EvaluationRepository;
import com.hackathon.repository.CategoryRoundRepository;
import com.hackathon.service.EvaluationAuditLogService;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
// Lưu và truy xuất lịch sử từng lần giám khảo chấm hoặc chấm lại bài nộp.
public class EvaluationAuditLogServiceImpl implements EvaluationAuditLogService {

    private final AuditService auditService;
    private final EvaluationAuditLogRepository evaluationAuditLogRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EvaluationRepository evaluationRepository;
    private final CategoryRoundRepository categoryRoundRepository;

    // Chụp lại trạng thái đánh giá và điểm từng tiêu chí sau mỗi lần giám khảo lưu kết quả.
    public void saveAttempt(
            Account actor,
            Evaluation evaluation,
            CriteriaType criteriaType,
            boolean reEvaluation
    ) {
        // Lấy bài nộp đang được đánh giá để xác định vòng và sự kiện liên quan.
        Submission submission = evaluation.getSubmission();
        // Đi từ bài nộp qua đội tham gia và danh mục vòng để lấy đúng vòng thi.
        Round round = submission.getTeamParticipant()
                .getCategoryRound()
                .getRound();

        // Lấy số thứ tự lớn nhất đã lưu của đúng đánh giá và loại tiêu chí.
        int previousAttempt = evaluationAuditLogRepository
                .findMaxAttemptNumber(
                        evaluation.getEvaluationId(), criteriaType);
        // Lần lưu mới luôn tăng một đơn vị so với lần gần nhất.
        int attemptNumber = previousAttempt + 1;

        // Chọn loại thao tác dựa trên đây là chấm lại, chấm lần đầu hay cập nhật lần chấm cũ.
        AuditAction action = reEvaluation
                ? AuditAction.RE_SUBMIT_EVALUATION
                : previousAttempt == 0
                ? AuditAction.SUBMIT_EVALUATION
                : AuditAction.UPDATE_EVALUATION;

        // Tạo nội dung lịch sử phù hợp với chấm lần đầu hoặc chấm lại.
        String description = reEvaluation
                ? String.format(
                        "Giám khảo đã lưu chấm lại phần %s của bài nộp %d.",
                        criteriaType, submission.getSubmissionId())
                : String.format(
                        "Giám khảo đã lưu phần %s của bài nộp %d.",
                        criteriaType, submission.getSubmissionId());

        // Lưu lịch sử chung trước để liên kết với bản chụp chi tiết của lần chấm.
        AuditLog auditLog = auditService.saveLog(
                actor,
                action,
                AuditEntityType.EVALUATION,
                evaluation.getEvaluationId(),
                description,
                null);

        // Tạo bản chụp thông tin tổng của lần đánh giá hiện tại.
        EvaluationAuditLog attempt = new EvaluationAuditLog();
        attempt.setAuditLog(auditLog);
        attempt.setEvaluation(evaluation);
        attempt.setEventId(round.getHackathonEvent().getEventId());
        attempt.setRoundId(round.getRoundId());
        attempt.setAttemptNumber(attemptNumber);
        attempt.setCriteriaType(criteriaType);
        attempt.setTotalScore(evaluation.getScore());
        attempt.setTotalComment(evaluation.getComment());
        attempt.setStatus(evaluation.getStatus());
        attempt.setCreatedAt(LocalDateTime.now());

        // Duyệt điểm chi tiết để chỉ lưu các tiêu chí thuộc đúng loại đang được chấm.
        for (EvaluationDetail detail : evaluation.getEvaluationDetails()) {
            // Lấy tiêu chí được gắn với dòng điểm chi tiết.
            EvaluationCriteria criteria = detail.getEvaluationCriteria();
            // Bỏ qua dữ liệu chi tiết không còn liên kết với tiêu chí.
            if (criteria == null) {
                continue;
            }
            // Không lưu tiêu chí thuộc phần chấm khác với loại đang xử lý.
            if (criteria.getType() != criteriaType) {
                continue;
            }

            // Tạo bản chụp điểm, nhận xét và trọng số tại thời điểm giám khảo lưu.
            EvaluationDetailAuditLog detailLog = new EvaluationDetailAuditLog();
            detailLog.setEvaluationAuditLog(attempt);
            detailLog.setEvaluationDetailId(detail.getId());
            detailLog.setCriteriaId(criteria.getEvaluationCriteriaId());
            detailLog.setCriteriaName(criteria.getCriteriaName());
            detailLog.setScore(detail.getScore());
            detailLog.setComment(detail.getComment());
            detailLog.setCriteriaWeight(criteria.getWeight());
            // Gắn dòng chi tiết vào lần chấm để được lưu cùng bản ghi cha.
            attempt.getDetails().add(detailLog);
        }

        // Lưu lần chấm và toàn bộ chi tiết đi kèm vào cơ sở dữ liệu.
        evaluationAuditLogRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<EvaluationAuditAttemptResponse> getEvaluationAttempts(
            Account account,
            Integer evaluationId
    ) {
        // Lấy các lần chấm của đánh giá theo thứ tự lần mới nhất trước.
        List<EvaluationAuditLog> attempts = evaluationAuditLogRepository
                .findByEvaluation_EvaluationIdOrderByAttemptNumberDesc(
                        evaluationId);

        // Không có lịch sử thì trả danh sách rỗng mà không cần xử lý tiếp.
        if (attempts.isEmpty()) {
            return List.of();
        }

        // Chỉ ban tổ chức mới được xem lịch sử thay đổi điểm.
        requireEventCoordinator(account);

        return attempts.stream()
                .map(attempt -> EvaluationAuditAttemptResponse.builder()
                        .attemptId(attempt.getId())
                        .evaluationId(attempt.getEvaluation().getEvaluationId())
                        .eventId(attempt.getEventId())
                        .roundId(attempt.getRoundId())
                        .attemptNumber(attempt.getAttemptNumber())
                        .criteriaType(attempt.getCriteriaType())
                        .totalScore(attempt.getTotalScore())
                        .totalComment(attempt.getTotalComment())
                        .status(attempt.getStatus())
                        .action(attempt.getAuditLog().getAction())
                        .actorName(attempt.getAuditLog().getActorName())
                        .createdAt(attempt.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationAuditListResponse> getEvaluations(
            Account account,
            Integer categoryRoundId
    ) {
        // Xác nhận danh mục vòng cần xem lịch sử thật sự tồn tại.
        categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hạng mục thuộc vòng thi"));
        // Kiểm tra tài khoản có quyền ban tổ chức trước khi đọc dữ liệu đánh giá.
        requireEventCoordinator(account);

        return evaluationRepository.findForAuditByCategoryRound(categoryRoundId)
                .stream()
                .map(evaluation -> {
                    Submission submission = evaluation.getSubmission();
                    Expert expert = evaluation.getExpertAssign().getExpert();
                    CategoryRound categoryRound = submission.getTeamParticipant()
                            .getCategoryRound();

                    return EvaluationAuditListResponse.builder()
                            .evaluationId(evaluation.getEvaluationId())
                            .submissionId(submission.getSubmissionId())
                            .teamId(submission.getTeam().getTeamId())
                            .teamName(submission.getTeam().getTeamName())
                            .judgeId(expert.getExpertId())
                            .judgeName(expert.getExpertName())
                            .categoryRoundId(categoryRound.getCategoryRoundId())
                            .categoryName(categoryRound.getCategory().getCategoryName())
                            .currentScore(evaluation.getScore())
                            .currentStatus(evaluation.getStatus())
                            .totalAttempts(evaluationAuditLogRepository
                                    .countByEvaluation_EvaluationId(
                                            evaluation.getEvaluationId()))
                            .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvaluationDetailAuditResponse> getAttemptDetails(
            Account account,
            Integer attemptId
    ) {
        // Tìm đúng lần chấm cần xem chi tiết theo mã lịch sử.
        EvaluationAuditLog attempt = evaluationAuditLogRepository
                .findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lần chấm điểm"));

        // Chỉ ban tổ chức được xem điểm chi tiết trong lịch sử chấm.
        requireEventCoordinator(account);

        return attempt.getDetails().stream()
                .map(detail -> EvaluationDetailAuditResponse.builder()
                        .detailAuditId(detail.getId())
                        .attemptId(attempt.getId())
                        .evaluationDetailId(detail.getEvaluationDetailId())
                        .criteriaId(detail.getCriteriaId())
                        .criteriaName(detail.getCriteriaName())
                        .score(detail.getScore())
                        .comment(detail.getComment())
                        .criteriaWeight(detail.getCriteriaWeight())
                        .build())
                .toList();
    }

    private void requireEventCoordinator(Account account) {
        // Không thể kiểm tra quyền nếu phiên làm việc không có tài khoản.
        if (account == null) {
            throw new BadRequestException("Không tìm thấy tài khoản đăng nhập");
        }

        // Tài khoản phải có hồ sơ ban tổ chức mới vượt qua bước kiểm tra quyền.
        eventCoordinatorRepository
                .findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải Event Coordinator"));
    }
}
