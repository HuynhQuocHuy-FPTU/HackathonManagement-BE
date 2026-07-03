package com.hackathon.controller;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.LuckyDrawResultService;
import com.hackathon.service.WorkshopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/draw-results")
@RequiredArgsConstructor
public class DrawResultController {

    private final LuckyDrawResultService luckyDrawResultService;
    private final WorkshopService workshopService;

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> importDrawResults(
            @PathVariable Integer eventId,
            @RequestBody List<DrawResultRequestDTO> drawResults,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Integer responseDeadline
    ) {

        luckyDrawResultService.importDrawResults(
                eventId,
                drawResults,
                userDetails,
                responseDeadline
        );

        return ResponseEntity.ok(
                ApiResponse.success(null, "Import kết quả bốc thăm thành công")
        );
    }

    @PatchMapping("/workshop/complete")
    public ResponseEntity<ApiResponse<Void>> completeWorkshop(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        workshopService.completedWorkshop(eventId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Workshop đã được đánh dấu hoàn thành thành công")
        );
    }

    @PatchMapping("/workshop/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelWorkshop(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        workshopService.cancelWorkshop(eventId, userDetails);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Workshop đã được hủy thành công")
        );
    }

}
