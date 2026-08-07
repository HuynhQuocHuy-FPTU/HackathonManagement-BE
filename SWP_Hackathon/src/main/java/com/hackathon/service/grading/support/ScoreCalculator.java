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

    /**
     * Tính toán tổng điểm dựa trên công thức Bình quân gia quyền (Weighted Average).
     * Công thức chuẩn hóa: Tổng điểm = Σ(Điểm thành phần * Trọng số) / Σ(Trọng số của các tiêu chí thực tế đã chấm)
     * Giải quyết triệt để lỗi sai hệ số khi chấm điểm từng phần (Partial Evaluation) hoặc cấu hình DB.
     */
    public BigDecimal calculateWeightedTotal(Collection<EvaluationDetail> details) {
        BigDecimal totalWeightedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO; // Tổng trọng số thực tế của các tiêu chí đã được chấm

        for (EvaluationDetail detail : details) {
            if (detail.getScore() == null) continue;

            // Trích xuất trọng số từ tiêu chí, mặc định là 0 nếu dữ liệu trống
            BigDecimal weight = (detail.getEvaluationCriteria() != null && detail.getEvaluationCriteria().getWeight() != null)
                    ? detail.getEvaluationCriteria().getWeight() : BigDecimal.ZERO;

            // 1. Cộng dồn tích số: Điểm số * Trọng số
            totalWeightedScore = totalWeightedScore.add(detail.getScore().multiply(weight));

            // 2. Cộng dồn tổng trọng số thực tế của các tiêu chí đã chấm
            totalWeight = totalWeight.add(weight);
        }

        // Chốt chặn bảo vệ: Nếu chưa có tiêu chí nào được chấm (Tổng trọng số = 0), trả về 0 để tránh lỗi chia cho 0
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // ĐIỀU CHỈNH CỐT LÕI: Chia cho tổng trọng số thực tế thay vì chia cho 100 cố định
        // Thực hiện làm tròn toán học về 2 chữ số thập phân (HALF_UP)
        return totalWeightedScore.divide(totalWeight, 2, RoundingMode.HALF_UP);
    }
}
