package com.hackathon.controller;

import com.hackathon.dto.analytics.MetricResultDTO;
import com.hackathon.dto.analytics.ReliabilityResultDTO;
import com.hackathon.dto.analytics.StudentCountResponse;
import com.hackathon.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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

    // ==========================================
    // NHÓM 3: EXPORT CSV ẨN DANH
    // ==========================================
    @GetMapping(value = "/events/{eventId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportEventCsv(@PathVariable Integer eventId) {
        return handleCsvExport("event", eventId);
    }

    @GetMapping(value = "/rounds/{roundId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportRoundCsv(@PathVariable Integer roundId) {
        return handleCsvExport("round", roundId);
    }

    @GetMapping(value = "/categories/{categoryId}/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCategoryCsv(@PathVariable Integer categoryId) {
        return handleCsvExport("category", categoryId);
    }

    // ==========================================
    // NHÓM 4: THỐNG KÊ SINH VIÊN (DASHBOARD)
    // ==========================================
    @GetMapping("/students/counts")
    public ResponseEntity<StudentCountResponse> getStudentCounts() {
        // Có thể bọc thêm ApiResponse nếu muốn đồng bộ với các Controller khác của bạn
        return ResponseEntity.ok(analyticsService.getStudentCounts());
    }

    // --- Hàm Helper dùng chung để tránh lặp code (DRY) ---
    private ResponseEntity<byte[]> handleCsvExport(String scope, Integer id) {
        byte[] csvData = analyticsService.exportAnonymizedCsv(scope, id);

        // Bắt lỗi nếu không có dữ liệu
        if (csvData.length == 0) {
            return ResponseEntity.noContent().build();
        }

        String fileName = String.format("rbl_anonymized_%s_%d.csv", scope, id);

        return ResponseEntity.ok()
                // Ép trình duyệt phải tải file xuống thay vì in ra màn hình
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvData);
    }
}