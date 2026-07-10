package com.hackathon.service.grading;

import com.hackathon.dto.evaluation.AssignedSubmissionForJudgeResponse;
import com.hackathon.dto.evaluation.EvaluationCriteriaResponse;
import com.hackathon.dto.evaluation.JudgeEvaluationResponse;
import com.hackathon.dto.evaluation.SubmitEvaluationRequest;
import com.hackathon.entity.Account;

import java.util.List;

/**
 * Khai báo giao diện dịch vụ quản lý luồng chấm điểm tiêu chuẩn trong hạn.
 */
public interface GradingService {
    JudgeEvaluationResponse submitOrUpdate(Account account, Integer submissionId, SubmitEvaluationRequest request);
    List<AssignedSubmissionForJudgeResponse> listAssignedSubmissions(Account account, Integer categoryRoundId);
    List<EvaluationCriteriaResponse> viewScoringCriteria(Integer roundId);
}