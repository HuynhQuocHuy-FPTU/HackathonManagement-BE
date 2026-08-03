package com.hackathon.service.grading;

import com.hackathon.dto.evaluation.*;
import com.hackathon.entity.Account;
import com.hackathon.entity.enums.CriteriaType;

import java.util.List;

/**
 * Khai báo giao diện dịch vụ quản lý luồng chấm điểm tiêu chuẩn trong hạn.
 */
public interface GradingService {
//  JudgeEvaluationResponse submitOrUpdate(Account account, Integer submissionId, SubmitEvaluationRequest request);

    JudgeDashboardResponse listAssignedSubmissions(
            Account account, Integer categoryRoundId);

    JudgeDashboardResponse listReEvaluationSubmissions(
            Account account, Integer categoryRoundId);

    List<EvaluationCriteriaResponse> viewScoringCriteria(Integer roundId);

    JudgeEvaluationResponse viewMyEvaluation(Account account, Integer submissionId);

    JudgeEvaluationResponse submitPartialEvaluation(Account account, Integer submissionId, SubmitEvaluationRequest request, CriteriaType targetType);

}
