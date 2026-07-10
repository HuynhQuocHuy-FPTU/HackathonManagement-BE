package com.hackathon.service.grading;

import com.hackathon.dto.evaluation.JudgeEvaluationResponse;
import com.hackathon.dto.evaluation.ReEvaluationRequest;
import com.hackathon.dto.evaluation.SubmitEvaluationRequest;
import com.hackathon.entity.Account;
import com.hackathon.security.CustomUserDetails;

/**
 * Khai báo giao diện dịch vụ quản lý luồng chấm điểm tiêu chuẩn trong hạn.
 */
public interface GradingService {
    JudgeEvaluationResponse submitOrUpdate(Account account, Integer submissionId, SubmitEvaluationRequest request);
//    JudgeEvaluationResponse updateEvaluation(Account account, Integer submissionId, SubmitEvaluationRequest request);
    JudgeEvaluationResponse reEvaluationSubmission(CustomUserDetails userDetails, ReEvaluationRequest request);
}