package com.hackathon.controller;

import com.hackathon.service.grading.ScoreExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ScoreExportController {

    private final ScoreExportService scoreExportService;

    @GetMapping("/category-rounds/{categoryRoundId}/scores.csv")
    public ResponseEntity<byte[]> exportAnonymizedScores(@PathVariable Integer categoryRoundId) {
        byte[] csvBytes = scoreExportService.exportAnonymizedScores(categoryRoundId);

        String filename = "scores_categoryRound_" + categoryRoundId + "_anonymized.csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(csvBytes);
    }
}