package com.hackathon.service.grading.support;

import com.hackathon.entity.Account;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.Submission;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Trách nhiệm: Ghi nhận nhật ký hệ thống (Audit Log) cho các tác vụ thay đổi điểm số.
 * Thiết kế theo dạng Wrapper để bọc AuditService cốt lõi, giúp việc gọi log ở Service chính trở nên gọn gàng và tường minh.
 */
@Component
@RequiredArgsConstructor
public class EvaluationAuditLogger {

    private final AuditService auditService;

    /**
     * Thực thi việc lưu vết (Tracking) quá trình thao tác điểm của Giám khảo.
     */
    public void logGraded(Account actor, Evaluation eval, Submission sub, Integer expertId, boolean isFirstTime, BigDecimal totalScore) {
        // 1. Phân loại hành động: Chấm mới (Insert) hay Cập nhật (Update)
        AuditAction action = isFirstTime ? AuditAction.SUBMIT_EVALUATION : AuditAction.UPDATE_EVALUATION;
        String actionText = isFirstTime ? "chấm điểm lần đầu" : "cập nhật điểm";

        // 2. Xây dựng nội dung mô tả chi tiết
        String description = String.format("Giám khảo (ExpertID: %d) đã %s cho Bài nộp (SubmissionID: %d). Tổng điểm ghi nhận: %s",
                expertId, actionText, sub.getSubmissionId(), totalScore.toString());

        // 3. Đẩy dữ liệu xuống dịch vụ Audit cốt lõi
        // Tham số bao gồm: Người thao tác, Hành động, Loại thực thể tác động, ID thực thể, và Mô tả chi tiết
        auditService.saveLog(actor, action, AuditEntityType.EVALUATION, eval.getEvaluationId(), description);
    }
}