package com.hackathon.service.grading.support;

import com.hackathon.entity.Round;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Trách nhiệm: Hiện thực hóa chính sách ràng buộc thời gian chấm điểm (Business Rules Enforcement).
 * Giúp mã nguồn tuân thủ nguyên tắc Open/Closed (OCP): Thay đổi luật tính deadline chỉ cần can thiệp tại đây.
 */
@Component
public class RoundEndTimeGradingPolicy {

    /**
     * Kiểm tra thời gian hiện tại có nằm trong khung thời gian cho phép chấm điểm của Vòng thi hay không.
     */
    public boolean isGradingOpen(Round round) {
        // Sử dụng giá trị EndTime của Round làm mốc hạn định thời gian chấm điểm mặc định
        return round.getEndTime() == null || LocalDateTime.now().isBefore(round.getEndTime());
    }
}