package com.hackathon.service.grading.support;

import com.hackathon.entity.EvaluationDetail;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Trách nhiệm: Thành phần tính toán chuyên biệt (Pure Function).
 * Đảm bảo tính nhất quán của công thức tính điểm toán học, cô lập hoàn toàn khỏi I/O và Database.
 */
@Component
public class ScoreCalculator {

    private static final BigDecimal DIVISOR_PERCENTAGE = BigDecimal.valueOf(100);

    /**
     * Tính toán tổng điểm dựa trên tổng tích số có trọng số của từng tiêu chí chi tiết.
     * Công thức: Tổng điểm = Σ(Điểm thành phần * Trọng số) / 100
     */
    public BigDecimal calculateWeightedTotal(Collection<EvaluationDetail> details) {
        BigDecimal totalWeightedScore = BigDecimal.ZERO;

        for (EvaluationDetail detail : details) {
            if (detail.getScore() == null) continue;

            BigDecimal weight = (detail.getEvaluationCriteria() != null && detail.getEvaluationCriteria().getWeight() != null)
                    ? detail.getEvaluationCriteria().getWeight() : BigDecimal.ZERO;

            // Tính toán tích số điểm và trọng số
            totalWeightedScore = totalWeightedScore.add(detail.getScore().multiply(weight));
        }

        // Chia quy đổi phần trăm và thực hiện làm tròn toán học về 2 chữ số thập phân (HALF_UP)
        return totalWeightedScore.divide(DIVISOR_PERCENTAGE, 2, RoundingMode.HALF_UP);
    }
}