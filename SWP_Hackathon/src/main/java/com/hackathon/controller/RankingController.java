package com.hackathon.controller;

import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.ExcelExportService;
import com.hackathon.service.ranking.RankingService;
import lombok.RequiredArgsConstructor;
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
     * Event Coordinator công bố nháp và mở cổng xếp hạng
     */

    @PostMapping("/{roundId}/publish-draft")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> publishDraftRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Integer hoursAmount) {
        rankingService.publishDraftRankingAndOpenAppeals(roundId, userDetails, hoursAmount);
        return ResponseEntity.ok(ApiResponse.success(null, "Ban tổ chức công bố bảng xếp hạng tạm thời thành công"));
    }

//    @PostMapping("/{roundId}/publish-final")
//    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
//    public ResponseEntity<ApiResponse<Void>> publishFinalRanking(
//            @PathVariable Integer roundId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//        rankingService.publishFinalRanking(roundId, userDetails);
//        return ResponseEntity.ok(ApiResponse.success(null, "Ban tổ chức công bố bảng xếp hạng chính thức thành công"));
//    }

    @GetMapping("/{roundId}/topN")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> getTopNRanking(
            @PathVariable Integer roundId) {
        CategoryRoundRankingResponse topN = rankingService.getTopNRanking(roundId);
        return ResponseEntity.ok(ApiResponse.success(topN, "Xem top N thành công."));
    }

    @GetMapping("/{roundId}/all")
    public ResponseEntity<ApiResponse<CategoryRoundRankingResponse>> getRanking(
            @PathVariable Integer roundId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CategoryRoundRankingResponse rank = rankingService.getRankingByAll(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(rank, "Xem ranking thành công."));
    }

    // Hàm này dành cho event muốn xuất file lúc nào cx được
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    @GetMapping("/coordinator/download-excel/{roundId}")
    public ResponseEntity<ApiResponse<String>> downloadRankingExcel(
            @PathVariable Integer roundId,
            @RequestParam String type) {
        String url = excelService.exportRankingToExcel(roundId, type);

        return ResponseEntity.ok(
                ApiResponse.success(url, "Export Excel thành công")
        );
    }

    @GetMapping("all/download-excel/{roundId}")
    public ResponseEntity<ApiResponse<String>> getRankingPublicExcels(
            @PathVariable Integer roundId,
            @RequestParam String type) {
        String url = rankingService.getRankingPublicExcels(roundId, type);

        return ResponseEntity.ok(
                ApiResponse.success(url, "Tải file ranking thành công")
        );
    }
}
