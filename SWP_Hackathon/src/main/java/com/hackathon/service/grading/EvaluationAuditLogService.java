package com.hackathon.service.grading;

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
import com.hackathon.service.AuditService;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationAuditLogService {

    private final AuditService auditService;
    private final EvaluationAuditLogRepository evaluationAuditLogRepository;
    private final EventCoordinatorRepository eventCoordinatorRepository;
    private final EvaluationRepository evaluationRepository;
    private final CategoryRoundRepository categoryRoundRepository;

    public void saveAttempt(
            Account actor,
            Evaluation evaluation,
            CriteriaType criteriaType,
            boolean reEvaluation
    ) {
        Submission submission = evaluation.getSubmission();
        Round round = submission.getTeamParticipant()
                .getCategoryRound()
                .getRound();

        int previousAttempt = evaluationAuditLogRepository
                .findMaxAttemptNumber(
                        evaluation.getEvaluationId(), criteriaType);
        int attemptNumber = previousAttempt + 1;

        AuditAction action = reEvaluation
                ? AuditAction.RE_SUBMIT_EVALUATION
                : previousAttempt == 0
                ? AuditAction.SUBMIT_EVALUATION
                : AuditAction.UPDATE_EVALUATION;

        String description = reEvaluation
                ? String.format(
                        "Giám khảo đã lưu chấm lại phần %s của bài nộp %d.",
                        criteriaType, submission.getSubmissionId())
                : String.format(
                        "Giám khảo đã lưu phần %s của bài nộp %d.",
                        criteriaType, submission.getSubmissionId());

        AuditLog auditLog = auditService.saveLog(
                actor,
                action,
                AuditEntityType.EVALUATION,
                evaluation.getEvaluationId(),
                description,
                null);

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

        for (EvaluationDetail detail : evaluation.getEvaluationDetails()) {
            EvaluationCriteria criteria = detail.getEvaluationCriteria();
            if (criteria == null) {
                continue;
            }
            if (criteria.getType() != criteriaType) {
                continue;
            }

            EvaluationDetailAuditLog detailLog = new EvaluationDetailAuditLog();
            detailLog.setEvaluationAuditLog(attempt);
            detailLog.setEvaluationDetailId(detail.getId());
            detailLog.setCriteriaId(criteria.getEvaluationCriteriaId());
            detailLog.setCriteriaName(criteria.getCriteriaName());
            detailLog.setScore(detail.getScore());
            detailLog.setComment(detail.getComment());
            detailLog.setCriteriaWeight(criteria.getWeight());
            attempt.getDetails().add(detailLog);
        }

        evaluationAuditLogRepository.save(attempt);
    }

    @Transactional(readOnly = true)
    public List<EvaluationAuditAttemptResponse> getEvaluationAttempts(
            Account account,
            Integer evaluationId
    ) {
        List<EvaluationAuditLog> attempts = evaluationAuditLogRepository
                .findByEvaluation_EvaluationIdOrderByAttemptNumberDesc(
                        evaluationId);

        if (attempts.isEmpty()) {
            return List.of();
        }

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
        categoryRoundRepository.findById(categoryRoundId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hạng mục thuộc vòng thi"));
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
        EvaluationAuditLog attempt = evaluationAuditLogRepository
                .findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lần chấm điểm"));

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
        if (account == null) {
            throw new BadRequestException("Không tìm thấy tài khoản đăng nhập");
        }

        eventCoordinatorRepository
                .findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new BadRequestException(
                        "Bạn không phải Event Coordinator"));
    }
}
