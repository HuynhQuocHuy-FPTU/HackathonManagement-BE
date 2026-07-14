package com.hackathon.controller;

import com.hackathon.dto.common.ApiResponse;
import com.hackathon.dto.evaluation.*;
import com.hackathon.entity.enums.CriteriaType;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.grading.GradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Controller chịu trách nhiệm định tuyến các yêu cầu liên quan tới tác vụ Chấm điểm của Giám khảo.
 * Tuân thủ cấu trúc RESTful API, đảm bảo cách ly logic nghiệp vụ hoàn toàn khỏi tầng vận chuyển HTTP.
 */
@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
@PreAuthorize("hasRole('EXPERT')") // Lớp bảo mật vòng ngoài: Giới hạn truy cập ở cấp độ phân hệ Role
public class GradingController {

    private final GradingService gradingService;

    /**
     * API: Hiển thị danh sách bài thi cho Giám khảo ở màn hình Dashboard
     * Cú pháp gọi endpoint: GET /api/grading/category-round/{categoryRoundId}/submissions
     */
    @GetMapping("/category-round/{categoryRoundId}/submissions")
    public ResponseEntity<ApiResponse<JudgeDashboardResponse>> listAssignedSubmissions(
            @PathVariable Integer categoryRoundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Gọi Service và ném kết quả vào ApiResponse chuẩn
        JudgeDashboardResponse responseData = gradingService.listAssignedSubmissions(userDetails.getAccount(), categoryRoundId);

        return ResponseEntity.ok(ApiResponse.ok("Kéo danh sách bài thi thành công", responseData));
    }

    /**
     * API: Tải danh sách bộ tiêu chí đánh giá của một Vòng thi
     * Cú pháp gọi endpoint: GET /api/grading/rounds/{roundId}/criteria
     * Phục vụ Frontend render Form chấm điểm động.
     */
    @GetMapping("/rounds/{roundId}/criteria")
    public ResponseEntity<ApiResponse<List<EvaluationCriteriaResponse>>> viewScoringCriteria(
            @PathVariable Integer roundId) {

        List<EvaluationCriteriaResponse> responseData = gradingService.viewScoringCriteria(roundId);

        return ResponseEntity.ok(ApiResponse.ok("Tải bộ tiêu chí đánh giá thành công", responseData));
    }

    /**
     * API: Chấm điểm phần BÀI NỘP (CODE / SUBMISSION)
     * Cú pháp: POST /api/grading/submissions/{submissionId}/evaluation/code
     */
    @PostMapping("/submissions/{submissionId}/evaluation/code")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>> evaluateCode(
            @PathVariable Integer submissionId,
            @Valid @RequestBody SubmitEvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Truyền thêm cờ CriteriaType.CODE (hoặc SUBMISSION tùy Enum của bạn)
        JudgeEvaluationResponse responseData = gradingService.submitPartialEvaluation(
                userDetails.getAccount(), submissionId, request, CriteriaType.SUBMISSION);

        return ResponseEntity.ok(ApiResponse.ok("Đã lưu điểm phần Code/Bài nộp thành công!", responseData));
    }

    /**
     * API: Chấm điểm phần THUYẾT TRÌNH (PRESENTATION)
     * Cú pháp: POST /api/grading/submissions/{submissionId}/evaluation/presentation
     */
    @PostMapping("/submissions/{submissionId}/evaluation/presentation")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>> evaluatePresentation(
            @PathVariable Integer submissionId,
            @Valid @RequestBody SubmitEvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Truyền thêm cờ CriteriaType.PRESENTATION
        JudgeEvaluationResponse responseData = gradingService.submitPartialEvaluation(
                userDetails.getAccount(), submissionId, request, CriteriaType.PRESENTATION);

        return ResponseEntity.ok(ApiResponse.ok("Đã lưu điểm phần Thuyết trình thành công!", responseData));
    }

    /**
     * API: Tải lại dữ liệu bài đã chấm (Review/Edit Mode)
     * Cú pháp gọi endpoint: GET /api/grading/submissions/{submissionId}/evaluation
     */
    @GetMapping("/submissions/{submissionId}/evaluation")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>> viewMyEvaluation(
            @PathVariable Integer submissionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        JudgeEvaluationResponse responseData = gradingService.viewMyEvaluation(userDetails.getAccount(), submissionId);

        return ResponseEntity.ok(ApiResponse.ok("Tải dữ liệu điểm cũ thành công!", responseData));
    }
//     Update điểm khi ban tổ chức từ chối xét duyệt
    @PostMapping("/submissions/{submissionId}/update/presentation")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>>updateEvaluationPresentation(
            @PathVariable Integer submissionId,
            @Valid @RequestBody SubmitEvaluationRequest request, // Khởi chạy cơ chế Validation Bean đập lỗi ngay tại cửa ngõ
            @AuthenticationPrincipal CustomUserDetails userDetails){
        JudgeEvaluationResponse executionResult = gradingService.updateEvaluation(
                userDetails.getAccount(),
                submissionId,
                request,
                CriteriaType.PRESENTATION
        );
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật điểm số  phần thuyết trình thành công!", executionResult));

    }

    @PostMapping("/submissions/{submissionId}/update/code")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>>updateEvaluationCode(
            @PathVariable Integer submissionId,
            @Valid @RequestBody SubmitEvaluationRequest request, // Khởi chạy cơ chế Validation Bean đập lỗi ngay tại cửa ngõ
            @AuthenticationPrincipal CustomUserDetails userDetails){
        JudgeEvaluationResponse executionResult = gradingService.updateEvaluation(
                userDetails.getAccount(),
                submissionId,
                request,
                CriteriaType.SUBMISSION
        );
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật điểm số đánh giá thành công!", executionResult));

    }

    // Chấm điểm lại khi có yêu cầu phúc khảo
    @PostMapping("/submissions/re-evaluation/code")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>>reEvaluationSubmissionCode(
            @Valid @RequestBody ReEvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        JudgeEvaluationResponse executionResult = gradingService.reEvaluationSubmission(userDetails, request,CriteriaType.SUBMISSION);
        return ResponseEntity.ok(ApiResponse.ok("Ban giám khảo chấm lại điểm số phúc khảo thành công!", executionResult));

    }
    @PostMapping("/submissions/re-evaluation/presentation")
    public ResponseEntity<ApiResponse<JudgeEvaluationResponse>>reEvaluationSubmissionPresentation(
            @Valid @RequestBody ReEvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        JudgeEvaluationResponse executionResult = gradingService.reEvaluationSubmission(userDetails, request,CriteriaType.PRESENTATION);
        return ResponseEntity.ok(ApiResponse.ok("Ban giám khảo chấm lại điểm số phúc khảo phần thuyết trình thành công!", executionResult));

    }
}