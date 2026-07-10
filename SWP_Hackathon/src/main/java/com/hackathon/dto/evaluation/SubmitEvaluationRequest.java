package com.hackathon.dto.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

/**
 * DTO đóng gói toàn bộ yêu cầu chấm điểm hoặc cập nhật điểm của Giám khảo.
 */
@Data
public class SubmitEvaluationRequest {

    // Nhận xét tổng quan của Giám khảo về toàn bộ bài nộp của đội thi
    private String comment;

    @NotEmpty(message = "Danh sách điểm số tiêu chí không được để trống")
    @Valid // Kích hoạt kiểm duyệt phân cấp (Cascade Validation) cho các phần tử con trong danh sách
    private List<CriteriaScoreRequest> criteriaScores;
}