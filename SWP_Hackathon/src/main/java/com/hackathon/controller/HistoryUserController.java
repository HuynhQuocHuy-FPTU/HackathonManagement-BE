package com.hackathon.controller;

import com.hackathon.dto.history.CriteriaHistoryResponse;
import com.hackathon.dto.history.ExpertHistoryResponse;
import com.hackathon.dto.history.StudentHistoryResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.impl.AccountServiceImpl;
import com.hackathon.service.CriteriaSetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/historty")
public class HistoryUserController {
    private final AccountServiceImpl accountService;
    private final CriteriaSetService criteriaSetService;
    @GetMapping("student/{studentId}")
    public ResponseEntity<ApiResponse<StudentHistoryResponse>> getHistoryStudent( @RequestParam(required = false) Integer studentId,
                                                                                 @AuthenticationPrincipal CustomUserDetails userDetails){
        StudentHistoryResponse response = accountService.studentHistory(studentId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem lịch sử của sinh viên thành công"));
    }

    @GetMapping("expert/{accountId}")
    public ResponseEntity<ApiResponse<ExpertHistoryResponse>> getHistoryExpert( @RequestParam(required = false) Integer accountId,
                                                                                 @AuthenticationPrincipal CustomUserDetails userDetails){
        ExpertHistoryResponse response = accountService.expertHistory(accountId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem lịch sử của chuyên gia thành công"));
    }

    @GetMapping("criteriaSet/{criteriaSetId}")
    public ResponseEntity<ApiResponse<CriteriaHistoryResponse>> getHistoryCriteria(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer criteriaSetId){
        CriteriaHistoryResponse response = criteriaSetService.getHistoryCriteria( userDetails, criteriaSetId );
        return ResponseEntity.ok(ApiResponse.success(response, "Xem lịch sử của chuyên gia thành công"));
    }
}
