package com.hackathon.controller;

import com.hackathon.dto.PublicStatisticsResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.service.PublicStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events/public/statistics")
@RequiredArgsConstructor
public class PublicStatisticsController {

    private final PublicStatisticsService publicStatisticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<PublicStatisticsResponse>>
    getPublicStatistics() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        publicStatisticsService.getPublicStatistics(),
                        "Lấy thống kê tổng quan thành công"
                )
        );
    }
}
