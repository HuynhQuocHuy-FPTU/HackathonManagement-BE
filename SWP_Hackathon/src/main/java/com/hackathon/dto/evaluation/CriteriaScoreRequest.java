package com.hackathon.dto.evaluation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO đại diện cho dữ liệu điểm số của từng tiêu chí đơn lẻ gửi từ Frontend.
 * Đảm bảo cơ chế kiểm tra dữ liệu đầu vào (Validation) ở mức trường.
 */
@Data
public class CriteriaScoreRequest {

    @NotNull(message = "Mã tiêu chí đánh giá không được để trống")
    private Integer evaluationCriteriaId;

    @NotNull(message = "Điểm số không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm số thấp nhất phải đạt 0.0")
    private BigDecimal score;

    private String comment;
}