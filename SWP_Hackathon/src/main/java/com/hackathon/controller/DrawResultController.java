package com.hackathon.controller;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.TeamParticipation;
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
    public ResponseEntity<List<TeamParticipation>> importDrawResults(
            @PathVariable Integer eventId,
            @RequestBody DrawResultRequestDTO drawResults,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Integer responseDeadline) {
        return ResponseEntity.ok(luckyDrawResultService.importDrawResults(eventId, drawResults, userDetails, responseDeadline));
    }

    @PatchMapping("/workshop/complete")
    public ResponseEntity<String> completeWorkshop(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        workshopService.completedWorkshop(eventId, userDetails);
        return ResponseEntity.ok("Workshop đã được đánh dấu hoàn thành thành công!");
    }

    @PatchMapping("/workshop/cancel")
    public ResponseEntity<String> cancelWorkshop(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        workshopService.cancelWorkshop(eventId, userDetails);
        return ResponseEntity.ok("Workshop đã được hủy thành công!");
    }

}
