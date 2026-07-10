package com.hackathon.service.grading.support;

import com.hackathon.dto.evaluation.CriteriaScoreResponse;
import com.hackathon.dto.evaluation.JudgeEvaluationResponse;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.EvaluationDetail;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Trách nhiệm: Đảm nhiệm việc ánh xạ (Mapping) dữ liệu từ Entity (Database) sang DTO (Presentation).
 * Áp dụng SRP (Single Responsibility Principle) để cô lập logic chuyển đổi dữ liệu khỏi luồng xử lý nghiệp vụ chính.
 */
@Component
public class EvaluationMapper {

    /**
     * Chuyển đổi thực thể Evaluation thành DTO JudgeEvaluationResponse hoàn chỉnh.
     */
    public JudgeEvaluationResponse toResponse(Evaluation evaluation, boolean isEditable) {

        // 1. Ánh xạ danh sách các điểm chi tiết thành phần
        List<CriteriaScoreResponse> criteriaScores = evaluation.getEvaluationDetails().stream()
                .map(this::mapCriteriaDetail)
                .collect(Collectors.toList());

        // 2. Đóng gói đối tượng phản hồi tổng thể
        return JudgeEvaluationResponse.builder()
                .evaluationId(evaluation.getEvaluationId())
                .submissionId(evaluation.getSubmission().getSubmissionId())
                // Lấy tên Team thông qua quan hệ: Evaluation -> Submission -> Team
                .teamName(evaluation.getSubmission().getTeam().getTeamName())
                .totalScore(evaluation.getScore())
                .comment(evaluation.getComment())
                .status(evaluation.getStatus())
                .isEditable(isEditable)
                .criteriaScores(criteriaScores)
                .build();
    }

    /**
     * Hàm Helper: Ánh xạ chi tiết một bản ghi EvaluationDetail sang DTO CriteriaScoreResponse.
     */
    private CriteriaScoreResponse mapCriteriaDetail(EvaluationDetail detail) {
        return CriteriaScoreResponse.builder()
                .evaluationCriteriaId(detail.getEvaluationCriteria().getEvaluationCriteriaId())
                .criteriaName(detail.getEvaluationCriteria().getCriteriaName())
                .type(detail.getEvaluationCriteria().getType())
                .weight(detail.getEvaluationCriteria().getWeight())
                .score(detail.getScore())
                .comment(detail.getComment())
                .build();
    }
}