package com.hackathon.dto.evaluation;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO chứa thông tin chi tiết điểm và nhận xét của một giám khảo khác cho một tiêu chí cụ thể.
 */
@Data
@Builder
public class OtherJudgeScoreDetailDTO {
    private Integer expertId;     // ID của giám khảo
    private String expertName;    // Tên hiển thị của giám khảo
    private BigDecimal score;     // Điểm số họ chấm cho tiêu chí này
    private String comment;       // Nhận xét của họ
}