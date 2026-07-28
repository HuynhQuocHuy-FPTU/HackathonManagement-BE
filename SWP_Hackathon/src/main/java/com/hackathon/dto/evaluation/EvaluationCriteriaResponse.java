package com.hackathon.dto.evaluation;

import com.hackathon.entity.enums.CriteriaType;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

/**
 * DTO này đóng vai trò vận chuyển dữ liệu cấu trúc bộ tiêu chí của Vòng thi.
 * FE sẽ dùng cái này để render ra các ô Input nhập điểm tương ứng.
 */
@Data
@Builder
public class EvaluationCriteriaResponse {
    private Integer evaluationCriteriaId;

    // Tên tiêu chí (VD: "Kiến trúc hệ thống", "Thuyết trình")
    private String criteriaName;

    // Trọng số (VD: 20%, 30%). FE dùng để hiển thị cho Giám khảo biết câu nào nhiều điểm
    private BigDecimal weight;

    // Mô tả chi tiết để Giám khảo đọc hiểu yêu cầu
    private String description;

    // Loại tiêu chí (VD: CODE, PRESENTATION) để FE phân nhóm (Group) thành các Tab
    private CriteriaType type;

    private Integer maxScore;

}