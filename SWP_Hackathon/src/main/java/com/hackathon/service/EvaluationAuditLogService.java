package com.hackathon.service;

import com.hackathon.dto.evaluation.EvaluationAuditAttemptResponse;
import com.hackathon.dto.evaluation.EvaluationAuditListResponse;
import com.hackathon.dto.evaluation.EvaluationDetailAuditResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.enums.CriteriaType;

import java.util.List;

public interface EvaluationAuditLogService {
    void saveAttempt(
            Account actor,
            Evaluation evaluation,
            CriteriaType criteriaType,
            boolean reEvaluation
    );

    List<EvaluationAuditAttemptResponse> getEvaluationAttempts(
            Account account,
            Integer evaluationId
    );

    List<EvaluationAuditListResponse> getEvaluations(
            Account account,
            Integer categoryRoundId
    );

    List<EvaluationDetailAuditResponse> getAttemptDetails(
            Account account,
            Integer attemptId
    );



}
