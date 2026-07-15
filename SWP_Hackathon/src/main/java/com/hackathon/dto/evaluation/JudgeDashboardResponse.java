package com.hackathon.dto.evaluation;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JudgeDashboardResponse {
    private LocalDateTime gradingDeadline; // Thời gian hết hạn để UI chạy đồng hồ đếm ngược
    private boolean isGradingOpen;         // Cờ trạng thái để UI biết hiển thị "Đang mở" hay "Đã đóng"
    private List<AssignedSubmissionForJudgeResponse> submissions; // Danh sách bài thi như cũ
}