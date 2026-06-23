package com.hackathon.controller;

import com.hackathon.dto.history.ExpertHistoryResponse;
import com.hackathon.dto.history.StudentHistoryResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.AccountServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/historty")
public class HistoryUserController {
    private final AccountServiceImpl accountService;
    @GetMapping("student/{accountId}/")
    public ResponseEntity<ApiResponse<StudentHistoryResponse>> getHistoryStudent(@PathVariable Integer accountId,
                                                                                 @AuthenticationPrincipal CustomUserDetails userDetails){
        StudentHistoryResponse response = accountService.studentHistory(accountId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem lịch sử của sinh viên thành công"));
    }

    @GetMapping("expert/{accountId}/")
    public ResponseEntity<ApiResponse<ExpertHistoryResponse>> getHistoryExpert(@PathVariable Integer accountId,
                                                                                 @AuthenticationPrincipal CustomUserDetails userDetails){
        ExpertHistoryResponse response = accountService.expertHistory(accountId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem lịch sử của Expert thành công"));
    }
}
