package com.hackathon.controller;

import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.OpenAppealRequestDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.ExcelExportService;
import com.hackathon.service.ParticipantService;
import com.hackathon.service.ranking.RankingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ranking/rounds")
public class RankingController {
    private final ExcelExportService excelService;
    private final RankingService rankingService;

    /**
     * View danh sách ranking dành cho Event Coordinator
     *
     */
    @GetMapping("/{roundId}")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> getRankingByEventCoordinator(
            @PathVariable("roundId") Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CategoryRoundRankingResponse response = rankingService.getRankingByEventCoordinator(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức xem dah sách ranking của vòng thi thành công"));
    }

    /**
     * Event Coordinator mở cổng đăng ký khiếu nại
     */
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    @PutMapping("/open-appeals")
    public ResponseEntity<ApiResponse<Void>> openAppeals(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OpenAppealRequestDTO request) {
        rankingService.openAppeals(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Mở cổng phúc khảo thành công"));
    }

    /**
     * Event Coordinator bấm nút phê duyệt dữ liệu xếp hạng
     */

    @PostMapping("/{roundId}/approve")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> approveRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CategoryRoundRankingResponse dto = rankingService.approveRanking(userDetails, roundId);

        return ResponseEntity.ok(ApiResponse.success(dto, "Ban tổ chức phê duyệt thành công"));
    }

    @PostMapping("/{roundId}/reject")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")

    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> rejectRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CategoryRoundRankingResponse dto = rankingService.rejectRanking(userDetails, roundId);

        return ResponseEntity.ok(ApiResponse.success(dto, "Ban tổ chức từ chối phê duyệt thành công"));
    }

    @PostMapping("/{roundId}/publish-draft")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> publishDraftRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        rankingService.publishDraftRanking(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Ban tổ chức công bố bảng xếp hạng tạm thời thành công"));
    }

    @PostMapping("/{roundId}/publish-final")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> publishFinalRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        rankingService.publishFinalRanking(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Ban tổ chức công bố bảng xếp hạng chính thức thành công"));
    }

    @GetMapping("/{roundId}/topN")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> getTopNRanking(
            @PathVariable Integer roundId) {
        CategoryRoundRankingResponse topN = rankingService.getTopNRanking(roundId);
        return ResponseEntity.ok(ApiResponse.success(topN, "Xem top N thành công."));
    }
    @GetMapping("/download-excel/{roundId}")
    public ResponseEntity<ApiResponse<String>> downloadRankingExcel(
            @PathVariable Integer roundId) {

        String url = excelService.exportRankingToExcel(roundId);

        return ResponseEntity.ok(
                ApiResponse.success(url, "Export Excel thành công")
        );
    }
}
