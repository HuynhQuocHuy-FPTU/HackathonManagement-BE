package com.hackathon.service.grading.support;

import com.hackathon.dto.evaluation.CriteriaScoreResponse;
import com.hackathon.dto.evaluation.JudgeEvaluationResponse;
import com.hackathon.dto.evaluation.OtherJudgeScoreDetailDTO;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.EvaluationDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Trách nhiệm: Ánh xạ dữ liệu và xử lý các phép toán Hiệu chuẩn (Calibration) điểm số.
 */
@Component
public class EvaluationMapper {

    // CẤU HÌNH NGƯỠNG ĐỘ LỆCH THEO PHẦN TRĂM (%)
    private static final BigDecimal TOTAL_DEVIATION_PERCENT_THRESHOLD = new BigDecimal("20.0"); // Ngưỡng tổng điểm
    private static final BigDecimal CRITERIA_DEVIATION_PERCENT_THRESHOLD = new BigDecimal("30.0"); // Ngưỡng từng tiêu chí

    public JudgeEvaluationResponse toResponse(Evaluation evaluation, boolean isEditable,
                                              LocalDateTime gradingDeadline, List<Evaluation> otherEvaluations) {

        BigDecimal myTotalScore = evaluation.getScore();
        BigDecimal averageOtherTotal = null;
        BigDecimal totalDeviation = null;
        BigDecimal totalDeviationPercentage = null;
        boolean hasTotalWarning = false;
        String warningMessage = null;

        // Trích xuất thang điểm tối đa (Max Score) từ cấu hình tiêu chí
        BigDecimal maxScore = BigDecimal.valueOf(100); // Mặc định an toàn
        if (evaluation.getEvaluationDetails() != null && !evaluation.getEvaluationDetails().isEmpty()) {
            maxScore = BigDecimal.valueOf(evaluation.getEvaluationDetails().get(0).getEvaluationCriteria().getMaxScore());
        }

        // 1. TÍNH TOÁN HIỆU CHUẨN TỔNG ĐIỂM
        if (otherEvaluations != null && !otherEvaluations.isEmpty()) {
            List<BigDecimal> otherTotalScores = otherEvaluations.stream()
                    .map(Evaluation::getScore)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            averageOtherTotal = calculateAverage(otherTotalScores);

            if (myTotalScore != null && averageOtherTotal != null && maxScore.compareTo(BigDecimal.ZERO) > 0) {
                totalDeviation = myTotalScore.subtract(averageOtherTotal).abs();

                // Công thức: (|Mình - Hội đồng| / MaxScore) * 100
                totalDeviationPercentage = totalDeviation
                        .divide(maxScore, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                if (totalDeviationPercentage.compareTo(TOTAL_DEVIATION_PERCENT_THRESHOLD) > 0) {
                    hasTotalWarning = true;
                    warningMessage = String.format("CẢNH BÁO: Tổng điểm của bạn (%s) đang lệch %s%% so với trung bình của hội đồng (%.2f).",
                            myTotalScore, totalDeviationPercentage.setScale(1, RoundingMode.HALF_UP), averageOtherTotal);
                }
            }
        }

        // 2. TÍNH TOÁN HIỆU CHUẨN TỪNG TIÊU CHÍ (Map chi tiết)
        List<CriteriaScoreResponse> criteriaScores = evaluation.getEvaluationDetails().stream()
                .map(detail -> mapCriteriaDetail(detail, otherEvaluations))
                .collect(Collectors.toList());

        return JudgeEvaluationResponse.builder()
                .evaluationId(evaluation.getEvaluationId())
                .submissionId(evaluation.getSubmission().getSubmissionId())
                .teamName(evaluation.getSubmission().getTeam().getTeamName())
                .totalScore(myTotalScore)
                .comment(evaluation.getComment())
                .status(evaluation.getStatus())
                .isEditable(isEditable)
                .gradingDeadline(gradingDeadline)

                // Trả về UI Dữ liệu Hiệu chuẩn Tổng điểm
                .averageOtherTotalScore(averageOtherTotal)
                .totalDeviation(totalDeviation)
                .totalDeviationPercentage(totalDeviationPercentage)
                .hasTotalDeviationWarning(hasTotalWarning)
                .deviationWarningMessage(warningMessage)

                .criteriaScores(criteriaScores)
                .build();
    }

    /**
     * Hàm Helper: Ánh xạ chi tiết tiêu chí và tích hợp điểm của hội đồng vào bên trong.
     */
    private CriteriaScoreResponse mapCriteriaDetail(EvaluationDetail detail, List<Evaluation> otherEvaluations) {
        Integer criteriaId = detail.getEvaluationCriteria().getEvaluationCriteriaId();
        BigDecimal myScore = detail.getScore();
        BigDecimal criteriaMaxScore = BigDecimal.valueOf(detail.getEvaluationCriteria().getMaxScore());

        BigDecimal averageOtherScore = null;
        BigDecimal criteriaDeviation = null;
        BigDecimal criteriaDeviationPercentage = null;
        boolean hasCriteriaWarning = false;

        List<OtherJudgeScoreDetailDTO> detailedOtherScores = new ArrayList<>();
        List<BigDecimal> justScoresForMath = new ArrayList<>();

        if (otherEvaluations != null && !otherEvaluations.isEmpty()) {
            for (Evaluation e : otherEvaluations) {
                e.getEvaluationDetails().stream()
                        .filter(d -> d.getEvaluationCriteria().getEvaluationCriteriaId() == (criteriaId))
                        .findFirst()
                        .ifPresent(d -> {
                            if (d.getScore() != null) {
                                detailedOtherScores.add(OtherJudgeScoreDetailDTO.builder()
                                        .expertId(e.getExpertAssign().getExpert().getExpertId())
                                        .expertName(e.getExpertAssign().getExpert().getExpertName())
                                        .score(d.getScore())
                                        .comment(d.getComment())
                                        .build());

                                justScoresForMath.add(d.getScore());
                            }
                        });
            }

            averageOtherScore = calculateAverage(justScoresForMath);

            if (myScore != null && averageOtherScore != null && criteriaMaxScore.compareTo(BigDecimal.ZERO) > 0) {
                criteriaDeviation = myScore.subtract(averageOtherScore).abs();

                criteriaDeviationPercentage = criteriaDeviation
                        .divide(criteriaMaxScore, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                if (criteriaDeviationPercentage.compareTo(CRITERIA_DEVIATION_PERCENT_THRESHOLD) > 0) {
                    hasCriteriaWarning = true;
                }
            }
        }

        return CriteriaScoreResponse.builder()
                .evaluationCriteriaId(criteriaId)
                .criteriaName(detail.getEvaluationCriteria().getCriteriaName())
                .type(detail.getEvaluationCriteria().getType())
                .weight(detail.getEvaluationCriteria().getWeight())
                .score(myScore)
                .comment(detail.getComment())

                .otherJudgesScores(detailedOtherScores)
                .averageOtherScore(averageOtherScore)
                .criteriaDeviation(criteriaDeviation)
                .criteriaDeviationPercentage(criteriaDeviationPercentage)
                .hasCriteriaDeviationWarning(hasCriteriaWarning)
                .build();
    }

    /**
     * Hàm Helper: Tính toán giá trị trung bình (Làm tròn 2 chữ số thập phân).
     */
    private BigDecimal calculateAverage(List<BigDecimal> scores) {
        if (scores == null || scores.isEmpty()) return null;
        BigDecimal sum = scores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);
    }
}
