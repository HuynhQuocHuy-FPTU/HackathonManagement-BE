package com.hackathon.controller;



import com.hackathon.dto.CriteriaVarianceDTO;
import com.hackathon.service.grading.GardingDashBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class GradingDashboardController {

    private final GardingDashBoardService gardingDashBoardService;

    @GetMapping("/category-rounds/{categoryRoundId}/criteria-variance")
    public ResponseEntity<List<CriteriaVarianceDTO>> getCriteriaVariance(@PathVariable Integer categoryRoundId) {
        return ResponseEntity.ok(gardingDashBoardService.getCriteriaVarianceDashboard(categoryRoundId));
    }
}