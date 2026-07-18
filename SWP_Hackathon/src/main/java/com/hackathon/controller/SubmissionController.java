package com.hackathon.controller;

import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.dto.submission.ResultSubmissionResponse;
import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.entity.Submission;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.EvaluationDetailService;
import com.hackathon.service.submission.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;
    private final EvaluationDetailService evaluationDetailService;
    @Operation(summary = "Nộp bài cho 1 vòng thi")
    @PostMapping(value = "/{roundId}/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Submission>> submit(
            @PathVariable Integer roundId,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUserDetails userDetails){

        Submission submission = submissionService.createSubmission(roundId, githubUrl, userDetails, files);
        return ResponseEntity.ok(ApiResponse.success(submission, "Đã nộp bài thành công"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getAllSubmission(){
        return ResponseEntity.ok(ApiResponse.success(submissionService.getAllSubmission(), "Lấy danh sách các bài nộp thành công"));
    }
    @GetMapping("/leader/{roundId}")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> getSubmissionForLeader(@PathVariable Integer roundId, @AuthenticationPrincipal CustomUserDetails userDetails){
        List<SubmissionResponse> list = submissionService.getSubmissionForStudent(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách submision thành công"));
    }

    @GetMapping("/evluated/{categoryRound}")
    public ResponseEntity<ApiResponse<ResultSubmissionResponse>> getEvaluatedSubmission(@PathVariable Integer categoryRound, @AuthenticationPrincipal CustomUserDetails userDetails){
        return ResponseEntity.ok(ApiResponse.success(submissionService.getResultOfSubmission(userDetails, categoryRound), "Lấy danh sách submision thành công"));
    }

    @GetMapping("/{submissionId}/evaluatedDetail")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getEvaluationDetail(@PathVariable Integer submissionId){
        List<EvaluationResponse> list = evaluationDetailService.getEvaluated(submissionId);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy chi tiết bài nộp được chấm thành công"));
    }

    @PatchMapping("/choose-final/{submissionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> chooseFinalSubmission(@PathVariable Integer submissionId, @AuthenticationPrincipal CustomUserDetails userDetails){
        submissionService.chooseFinalSubmission(submissionId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã chọn bài nộp thành công"));
    }

    @PatchMapping("/set-not-final/{submissionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> setNotFinalSubmission(
            @PathVariable Integer submissionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        submissionService.setNotFinal(submissionId, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Đã bỏ chọn bài nộp chính thức thành công"
                )
        );
    }

}
