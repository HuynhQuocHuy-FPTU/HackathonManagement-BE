package com.hackathon.service.grading;

import com.hackathon.dto.evaluation.*;
import com.hackathon.entity.Account;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

/**
 * Khai báo giao diện dịch vụ quản lý luồng chấm điểm tiêu chuẩn trong hạn.
 */
public interface GradingService {
    JudgeEvaluationResponse submitOrUpdate(Account account, Integer submissionId, SubmitEvaluationRequest request);

    List<AssignedSubmissionForJudgeResponse> listAssignedSubmissions(Account account, Integer categoryRoundId);

    List<EvaluationCriteriaResponse> viewScoringCriteria(Integer roundId);

    JudgeEvaluationResponse viewMyEvaluation(Account account, Integer submissionId);

    JudgeEvaluationResponse reEvaluationSubmission(CustomUserDetails userDetails, ReEvaluationRequest request);

    JudgeEvaluationResponse updateEvaluation(Account account, Integer submissionId, SubmitEvaluationRequest request);

}