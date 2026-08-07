package com.hackathon.controller;

import com.hackathon.dto.evaluation.EvaluationAuditAttemptResponse;
import com.hackathon.dto.evaluation.EvaluationDetailAuditResponse;
import com.hackathon.dto.evaluation.EvaluationAuditListResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.impl.EvaluationAuditLogServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation-audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EVENTCOORDINATOR')")
public class EvaluationAuditLogController {

    private final EvaluationAuditLogServiceImpl evaluationAuditLogServiceImpl;

    @GetMapping("/category-rounds/{categoryRoundId}/evaluations")
    public ResponseEntity<ApiResponse<List<EvaluationAuditListResponse>>>
    getEvaluations(
            @PathVariable Integer categoryRoundId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<EvaluationAuditListResponse> response =
                evaluationAuditLogServiceImpl.getEvaluations(
                        userDetails.getAccount(), categoryRoundId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy danh sách bảng chấm theo hạng mục của vòng thi thành công",
                response));
    }

    @GetMapping("/evaluations/{evaluationId}")
    public ResponseEntity<ApiResponse<List<EvaluationAuditAttemptResponse>>>
    getEvaluationAttempts(
            @PathVariable Integer evaluationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<EvaluationAuditAttemptResponse> response =
                evaluationAuditLogServiceImpl.getEvaluationAttempts(
                        userDetails.getAccount(), evaluationId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy lịch sử các lần chấm thành công", response));
    }

    @GetMapping("/{attemptId}/details")
    public ResponseEntity<ApiResponse<List<EvaluationDetailAuditResponse>>>
    getAttemptDetails(
            @PathVariable Integer attemptId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<EvaluationDetailAuditResponse> response =
                evaluationAuditLogServiceImpl.getAttemptDetails(
                        userDetails.getAccount(), attemptId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Lấy chi tiết điểm của lần chấm thành công", response));
    }
}
