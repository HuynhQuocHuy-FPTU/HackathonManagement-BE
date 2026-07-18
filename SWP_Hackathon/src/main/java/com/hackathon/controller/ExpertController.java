package com.hackathon.controller;

import com.hackathon.dto.categoryRound.CategoryRoundResponseDTO;
import com.hackathon.dto.expert.ExpertOverviewResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.CategoryRoundService;
import com.hackathon.service.ExpertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/expert")
public class ExpertController {
    private final CategoryRoundService categoryRoundService;
    private final ExpertService expertService;
    @GetMapping("/mentor/assigncategory-round/{eventId}")
    public ResponseEntity<ApiResponse<List<CategoryRoundResponseDTO>>>getAssignedCategoryRounds(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId){
        List<CategoryRoundResponseDTO> list = categoryRoundService.getAssignedCategoryRounds(userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(list, "Mentor xem tất cả các hạng mục mình được phân công thành công"));
    }
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<ExpertOverviewResponse>> getExpertOverview(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId){
        ExpertOverviewResponse rs = expertService.getExpertOverview(userDetails,eventId);
        return ResponseEntity.ok(ApiResponse.success(rs, "Xem danh sách tổng quan về chuyên gia thuộc về một sự kiện cụ thể thành công"));
    }
}
