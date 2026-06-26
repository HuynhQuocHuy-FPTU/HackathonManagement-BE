package com.hackathon.controller;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.ParticipantServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {
    private final ParticipantServiceImpl participantService;

    @GetMapping("/teams/{eventId}")
    public ResponseEntity<ApiResponse<List<ExpertAssignedGroupDTO>>> getAssignedGroups(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        participantService.getAssignParticipants(eventId, userDetails),
                        "Danh sách nhóm đã phân công"
                )
        );
    }

    @PutMapping("/teams/disqualify")
    public ResponseEntity<ApiResponse<Void>> disqualifyTeam(
            @RequestParam Integer eventId,
            @RequestParam Integer teamId,
            @RequestParam String reason
    ) {

        participantService.disqualifyTeam(eventId, teamId, reason);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Đã loại đội thi khỏi sự kiện"
                )
        );
    }
}
