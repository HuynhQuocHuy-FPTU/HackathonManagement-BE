package com.hackathon.service.grading.support;

import com.hackathon.entity.Round;
import com.hackathon.exception.BadRequestException;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Trách nhiệm: Quản lý thời gian khóa sổ chấm điểm.
 * Logic: Deadline chấm điểm = Deadline nộp bài (Round EndTime) + X giờ.
 */
@Component
public class RoundEndTimeGradingPolicy {

    // CẤU HÌNH SỐ GIỜ GIÁM KHẢO ĐƯỢC CHẤM SAU KHI VÒNG THI KẾT THÚC
    private static final int EXTRA_HOURS_FOR_GRADING = 2;

    /**
     * Tính toán Deadline chính xác cho Giám khảo
     */
    public LocalDateTime getGradingDeadline(Round round) {
        if (round.getSubmissionDeadline() == null) {
            return null; // Nếu vòng thi không setup giờ kết thúc -> Chấm vô thời hạn
        }
        // Lấy giờ kết thúc vòng thi cộng thêm số giờ cấu hình
        return round.getSubmissionDeadline().plusHours(EXTRA_HOURS_FOR_GRADING);
    }

    /**
     * Kiểm tra xem còn trong thời gian chấm không
     */
    public boolean isGradingOpen(Round round) {
        LocalDateTime deadline = getGradingDeadline(round);
        if(LocalDateTime.now().isBefore(round.getSubmissionDeadline())) throw new BadRequestException("Chưa đến thời gian chấm bài");
        return deadline == null || LocalDateTime.now().isBefore(deadline);
    }
}