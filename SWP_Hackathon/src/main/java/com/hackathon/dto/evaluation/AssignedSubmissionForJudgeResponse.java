package com.hackathon.dto.evaluation;

import com.hackathon.dto.submission.FileDTO;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO này đóng vai trò vận chuyển dữ liệu để Frontend vẽ danh sách (Table/Grid)
 * các bài thi ngoài màn hình Dashboard của Giám khảo.
 */
@Data
@Builder
public class AssignedSubmissionForJudgeResponse {
    private Integer submissionId;
    private String teamName;
    private String description;
    private String githubUrl;

    // Ngày sinh viên nộp bài (FE cần để hiển thị time)
    private LocalDateTime submittedAt;

    // 2 cờ cực kỳ quan trọng để FE biết trạng thái:
    // "NOT_GRADED" -> Hiện nút màu xanh "Chấm ngay"
    // "GRADED" -> Hiện nút màu xám "Xem/Sửa điểm" và show tổng điểm ra
    private String myEvaluationStatus;
    private BigDecimal myTotalScore;
    private List<FileDTO> files;
}