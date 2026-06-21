package com.hackathon.controller;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.service.ParticipantServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantServiceImpl participantService;

    @GetMapping("/teams/{eventId}")
    public ResponseEntity<List<ExpertAssignedGroupDTO>> getAssignedGroups(@PathVariable Integer eventId) {
        return ResponseEntity.ok(participantService.getAssignParticipants(eventId));
    }
    @PutMapping("/teams/disqualify")
    public ResponseEntity<String> disqualifyTeam(
            @RequestParam Integer eventId,
            @RequestParam Integer teamId,
            @RequestParam String reason) {

        // Gọi service với đủ 3 tham số
        participantService.disqualifyTeam(eventId, teamId, reason);
        return ResponseEntity.ok("Đã loại đội thi ID: " + teamId + " khỏi sự kiện " + eventId + " với lý do: " + reason);
    }
}
