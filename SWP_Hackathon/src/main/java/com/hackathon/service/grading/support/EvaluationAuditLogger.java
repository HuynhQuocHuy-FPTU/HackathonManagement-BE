package com.hackathon.service.grading.support;

import com.hackathon.entity.Account;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.Submission;
import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import com.hackathon.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trách nhiệm: Ghi nhận nhật ký hệ thống (Audit Log) cho các tác vụ thay đổi điểm số.
 */
@Component
@RequiredArgsConstructor
public class EvaluationAuditLogger {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /**
     * GHI LOG LUỒNG CHẤM ĐIỂM THÔNG THƯỜNG
     */
    public void logGraded(Account actor, Evaluation eval, Submission sub, Integer expertId,
                          boolean isFirstTime, BigDecimal oldTotalScore, BigDecimal newTotalScore,
                          String oldComment, String newComment, List<Map<String, Object>> detailChanges) {

        AuditAction action = isFirstTime ? AuditAction.SUBMIT_EVALUATION : AuditAction.UPDATE_EVALUATION;
        String actionText = isFirstTime ? "chấm điểm lần đầu" : "cập nhật điểm";

        String oldScoreStr = oldTotalScore != null ? oldTotalScore.toString() : "0";
        String description = String.format("Giám khảo (ExpertID: %d) đã %s cho Bài nộp (SubmissionID: %d). Tổng điểm: %s -> %s",
                expertId, actionText, sub.getSubmissionId(), oldScoreStr, newTotalScore.toString());

        // Sử dụng LinkedHashMap để bảo toàn thứ tự Key khi xuất JSON
        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("oldTotalScore", oldTotalScore);
        auditData.put("newTotalScore", newTotalScore);
        auditData.put("oldTotalComment", oldComment);
        auditData.put("newTotalComment", newComment);
        auditData.put("details", detailChanges);

        String jsonData = null;
        try {
            jsonData = objectMapper.writeValueAsString(auditData);
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON Audit Log: " + e.getMessage());
        }

        auditService.saveLog(actor, action, AuditEntityType.EVALUATION, eval.getEvaluationId(), description, jsonData);
    }

    /**
     * GHI LOG LUỒNG CHẤM LẠI (PHÚC KHẢO)
     */
    public void logReEvaluation(Account actor, Evaluation eval, Submission sub, Integer expertId,
                                BigDecimal oldTotalScore, BigDecimal newTotalScore,
                                String oldComment, String newComment, List<Map<String, Object>> detailChanges) {

        String oldScoreStr = oldTotalScore != null ? oldTotalScore.toString() : "0";
        String description = String.format("Giám khảo (ExpertID: %d) đã CHẤM PHÚC KHẢO cho Bài nộp (SubmissionID: %d). Tổng điểm: %s -> %s",
                expertId, sub.getSubmissionId(), oldScoreStr, newTotalScore.toString());

        Map<String, Object> auditData = new LinkedHashMap<>();
        auditData.put("oldTotalScore", oldTotalScore);
        auditData.put("newTotalScore", newTotalScore);
        auditData.put("oldTotalComment", oldComment);
        auditData.put("newTotalComment", newComment);
        auditData.put("details", detailChanges);

        String jsonData = null;
        try {
            jsonData = objectMapper.writeValueAsString(auditData);
        } catch (Exception e) {
            System.err.println("Lỗi parse JSON Audit Log Phúc khảo: " + e.getMessage());
        }

        auditService.saveLog(actor, AuditAction.RE_SUBMIT_EVALUATION, AuditEntityType.EVALUATION, eval.getEvaluationId(), description, jsonData);
    }
}