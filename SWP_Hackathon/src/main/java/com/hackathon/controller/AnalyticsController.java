package com.hackathon.controller;

import com.hackathon.dto.analytics.MetricResultDTO;
import com.hackathon.dto.analytics.ReliabilityResultDTO;
import com.hackathon.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ==========================================
    // NHÓM 1: THỐNG KÊ TIÊU CHÍ (CRITERIA STATS)
    // ==========================================
    @GetMapping("/events/{eventId}/criteria-stats")
    public ResponseEntity<List<MetricResultDTO>> getEventCriteriaStats(@PathVariable Integer eventId) {
        return ResponseEntity.ok(analyticsService.getCriteriaStats("event", eventId));
    }

    @GetMapping("/rounds/{roundId}/criteria-stats")
    public ResponseEntity<List<MetricResultDTO>> getRoundCriteriaStats(@PathVariable Integer roundId) {
        return ResponseEntity.ok(analyticsService.getCriteriaStats("round", roundId));
    }

    @GetMapping("/submissions/{submissionId}/criteria-stats")
    public ResponseEntity<List<MetricResultDTO>> getSubmissionCriteriaStats(@PathVariable Integer submissionId) {
        return ResponseEntity.ok(analyticsService.getCriteriaStats("submission", submissionId));
    }

    // ==========================================
    // NHÓM 2: CHỈ SỐ ĐỘ TIN CẬY (ICC & ALPHA)
    // ==========================================
    @GetMapping("/events/{eventId}/reliability")
    public ResponseEntity<ReliabilityResultDTO> getEventReliabilityMetrics(@PathVariable Integer eventId) {
        return ResponseEntity.ok(analyticsService.calculateReliabilityMetrics(eventId));
    }
}