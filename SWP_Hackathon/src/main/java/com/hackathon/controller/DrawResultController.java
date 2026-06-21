package com.hackathon.controller;

import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.Participant;
import com.hackathon.service.DrawResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/draw-results")
@RequiredArgsConstructor
public class DrawResultController {

    private final DrawResultService drawResultService;

    @PutMapping
    public ResponseEntity<List<Participant>> importDrawResults(
            @PathVariable Integer eventId,
            @RequestBody List<DrawResultRequestDTO> drawResults) {
        return ResponseEntity.ok(drawResultService.importDrawResults(eventId, drawResults));
    }
}
