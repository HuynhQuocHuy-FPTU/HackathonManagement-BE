package com.hackathon.controller;

import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.ParticipantService;
import com.hackathon.service.ParticipantServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/ranking/rounds")
public class RankingController {
    private final ParticipantService participantService;

    /**
     * View danh sách ranking dành cho Event Coordinator
     *
     */
    @GetMapping("/{roundId}")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> getRankingByEventCoordinator(
            @PathVariable ("roundId") Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        CategoryRoundRankingResponse response = participantService.getRankingByEventCoordinator(roundId,userDetails);
        return ResponseEntity.ok(ApiResponse.success(response,"Ban tổ chức xem dah sách ranking của vòng thi thành công"));
    }
    /**
     * Event Coordinator bấm nút phê duyệt dữ liệu xếp hạng
     */

    @PostMapping("/{roundId}/approve")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> approveRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CategoryRoundRankingResponse dto = participantService.approveRanking(userDetails,roundId);

        return ResponseEntity.ok(ApiResponse.success(dto,"Ban tổ chức phê duyệt thành công"));
    }
    @PostMapping("/{rounId}/reject")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> rejectRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CategoryRoundRankingResponse dto = participantService.rejectRanking(userDetails,roundId);

        return ResponseEntity.ok(ApiResponse.success(dto,"Ban tổ chức từ chối phê duyệt thành công"));
    }
    @PostMapping("/{roundId}/publish-draft")
    public ResponseEntity<ApiResponse<Void>> publishDraftRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
         participantService.publishDraftRanking(roundId,userDetails);
        return ResponseEntity.ok(ApiResponse.success(null,"Ban tổ chức công bố bảng xếp hạng tạm thời thành công"));
    }
    @PostMapping("/{roundId}/publish-final")
    public ResponseEntity<ApiResponse<Void>> publishFinalRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        participantService.publishFinalRanking(roundId,userDetails);
        return ResponseEntity.ok(ApiResponse.success(null,"Ban tổ chức công bố bảng xếp hạng chính thức thành công"));
    }
}
