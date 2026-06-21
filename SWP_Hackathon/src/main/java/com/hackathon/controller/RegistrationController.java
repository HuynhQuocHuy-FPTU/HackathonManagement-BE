package com.hackathon.controller;

import com.hackathon.dto.notification.NotificationResponse;
import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.RegistrationEventService;
import com.hackathon.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
public class RegistrationController {
    @Autowired
    private TeamService teamService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RegistrationEventService registrationEventService;

    @GetMapping("/{eventId}/approveTeam")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getTeamsForApproval(@PathVariable Integer eventId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<RegistrationResponse> list = registrationEventService.getTeamsForApproval(eventId,userDetails);
        return ResponseEntity.ok(ApiResponse.success(list,"Lấy danh sách phê duyệt Team thành công."));
    }

    @GetMapping("/{registrationId}/approveTeam-detail")
    public ResponseEntity<ApiResponse<RegistrationResponse>> getTeamsDetailForApproval(@PathVariable Integer registrationId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        RegistrationResponse list = registrationEventService.getTeamsDetailForApproval(registrationId,userDetails);
        return ResponseEntity.ok(ApiResponse.success(list,"Lấy danh sách phê duyệt Team thành công."));
    }


    // Registration event
    @PostMapping("/{eventId}/register-event")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>>registrationEvent(@Valid@PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        registrationEventService.registerEvent(eventId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null,"Đăng ký sự kiện thành công"));
    }

    // Mời thêm thành viên vào Team đã có
    @PostMapping("/events/registration/teams/invite")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TeamResponse>> sendInvitation(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (request.getTeamId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.success(null, "MISSING_TEAM_ID"));
        }
        TeamResponse response = teamService.sendTeamInvitation(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã gửi lời mời thành công"));
    }


}