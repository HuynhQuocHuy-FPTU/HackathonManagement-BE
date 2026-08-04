package com.hackathon.dto.evaluation;

import com.hackathon.entity.enums.EvaluationStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tổng hợp kết quả đánh giá cuối cùng trả về cho Frontend sau khi xử lý lưu dữ liệu.
 */
@Data
@Builder
public class JudgeEvaluationResponse {
    private Integer evaluationId;
    private Integer submissionId;
    private String teamName;
    private BigDecimal totalScore; // Điểm tổng kết sau khi nhân trọng số
    private String comment;
    private EvaluationStatus status;
    private boolean isEditable;    // Trạng thái kiểm soát quyền chỉnh sửa dựa trên cấu hình thời gian
    private LocalDateTime gradingDeadline;

    // --- DỮ LIỆU HIỆU CHUẨN ĐỘ LỆCH (CALIBRATION) TỔNG ĐIỂM ---
    private BigDecimal averageOtherTotalScore;       // Tổng điểm trung bình của hội đồng
    private BigDecimal totalDeviation;               // Lệch tuyệt đối tổng điểm
    private BigDecimal totalDeviationPercentage;     // Phần trăm lệch tổng điểm
    private Boolean hasTotalDeviationWarning;        // Cờ báo đỏ cho Frontend (Lệch > 20%)
    private String deviationWarningMessage;          // Lời nhắc nhở hiển thị lên Popup

    // Danh sách các tiêu chí (đã bao gồm chi tiết của hội đồng bên trong)
    private List<CriteriaScoreResponse> criteriaScores;
}