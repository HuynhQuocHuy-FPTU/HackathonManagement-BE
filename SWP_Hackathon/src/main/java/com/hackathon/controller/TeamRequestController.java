package com.hackathon.controller;

import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.teamRequest.TeamRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/team-request")
@RequiredArgsConstructor
public class TeamRequestController {
    private final TeamRequestService teamRequestService;

    // Team gui request đến Mentor nhận sự hỗ trợ
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping()
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> teamSendRequestToMentor(
            @RequestParam String requestMessage,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamRequestResponse> response = teamRequestService.teamSendRequestToMentor(requestMessage, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Team Leader gửi yêu cầu nhận sự hỗ trợ tới Mentor thành công."));
    }

    //Expert nhận list các Request mà Team gửi đến
    @PreAuthorize("hasRole('EXPERT')")
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getTeamRequestsForExpert(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamRequestResponse> response = teamRequestService.getTeamRequestsForExpert(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Expert nhận danh sách các yêu cầu nhận sự hỗ trợ thành công."));
    }

    //  Chấp nhận
    @PatchMapping("/{requestId}/accept")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> acceptTeamRequest(
            @RequestParam String responseMessage,
            @PathVariable Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamRequestResponse response = teamRequestService.acceptTeamRequest(responseMessage, requestId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Chấp nhận yêu cầu nhận hỗ trợ thành công."));
    }

    //  Từ chối
    @PatchMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> rejectTeamRequest(
            @RequestParam String responseMessage,
            @PathVariable Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamRequestResponse response = teamRequestService.rejectTeamRequest(responseMessage, requestId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Từ chối yêu cầu nhận hỗ trợ thành công."));
    }
}
