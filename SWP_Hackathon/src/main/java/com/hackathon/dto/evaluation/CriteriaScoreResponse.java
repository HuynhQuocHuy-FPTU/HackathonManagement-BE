package com.hackathon.dto.evaluation;

import com.hackathon.entity.enums.CriteriaType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO định dạng dữ liệu trả về chi tiết cho từng tiêu chí đã được chấm.
 * Phục vụ tầng Presentation tái hiện cấu trúc bảng điểm trên giao diện.
 */
@Data
@Builder
public class CriteriaScoreResponse {
    private Integer evaluationCriteriaId;
    private String criteriaName;
    private CriteriaType type; // Phân loại tiêu chí (Giúp FE nhóm động theo tab)
    private BigDecimal weight; // Trọng số của tiêu chí
    private BigDecimal score;  // Điểm số thực tế
    private String comment;    // Ghi chú cụ thể cho tiêu chí từ Giám khảo

    // --- DỮ LIỆU HIỆU CHUẨN ĐỘ LỆCH (CALIBRATION) TIÊU CHÍ ---
    // Danh sách điểm chi tiết từ các giám khảo khác trong hội đồng
    private List<OtherJudgeScoreDetailDTO> otherJudgesScores;

    private BigDecimal averageOtherScore;            // Điểm trung bình của hội đồng
    private BigDecimal criteriaDeviation;            // Lệch tuyệt đối (|Mình - Hội đồng|)
    private BigDecimal criteriaDeviationPercentage;  // Phần trăm lệch so với thang điểm tối đa
    private Boolean hasCriteriaDeviationWarning;
}