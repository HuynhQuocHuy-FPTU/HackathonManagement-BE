package com.hackathon.service.grading.support;

import com.hackathon.entity.Round;
import com.hackathon.exception.BadRequestException;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Trách nhiệm: Quản lý thời gian khóa sổ chấm điểm.
 * Logic: Sử dụng thời gian kết thúc đánh giá đã cấu hình cho round.
 */
@Component
public class RoundEndTimeGradingPolicy {

    /**
     * Tính toán Deadline chính xác cho Giám khảo
     */
    public LocalDateTime getGradingDeadline(Round round) {
        return round.getEvaluationDeadline();
    }

    /**
     * Kiểm tra xem còn trong thời gian chấm không
     */
    public boolean isGradingOpen(Round round) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = getGradingDeadline(round);
        if (round.getSubmissionDeadline() != null
                && now.isBefore(round.getSubmissionDeadline())) {
            throw new BadRequestException("Chưa đến thời gian chấm bài");
        }
        return deadline == null || now.isBefore(deadline);
    }

    public void validateScoringTime(Round round, boolean isReEvaluation) {
        if (isReEvaluation) {
            LocalDateTime deadline = round.getResolveAppealDeadline();
            if (deadline == null) {
                throw new BadRequestException("Vòng thi chưa cấu hình thời hạn giải quyết khiếu nại.");
            }
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new BadRequestException("Đã hết thời hạn chấm điểm phúc khảo.");
            }
            return;
        }

        if (!isGradingOpen(round)) {
            throw new BadRequestException("Hệ thống đã khóa sổ dữ liệu chấm điểm do quá thời hạn quy định.");
        }
    }
}
