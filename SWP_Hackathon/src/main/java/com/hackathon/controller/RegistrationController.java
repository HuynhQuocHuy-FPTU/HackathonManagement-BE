package com.hackathon.controller;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.dto.registration.CountRegistrationDTO;
import com.hackathon.dto.registration.RegistrationResponse;
import com.hackathon.dto.registration.RegistrationHistoryResponse;
import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.InviteTeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.Registration;
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
@RequestMapping("/api/registrations")
public class RegistrationController {

    @Autowired
    private RegistrationEventService registrationEventService;

    @GetMapping("/history/current-team")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<RegistrationHistoryResponse>>> getCurrentTeamRegistrationHistory(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        registrationEventService.getCurrentTeamRegistrationHistory(userDetails),
                        "Lấy lịch sử đăng ký của đội hiện tại thành công"
                )
        );
    }

    // Registration event
    @PostMapping("/{eventId}/register-event")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>>registrationEvent(@Valid@PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        registrationEventService.registerEvent(eventId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null,"Đăng ký sự kiện thành công"));
    }

    // Lấy ra list team đã được approve
    @GetMapping("/{eventId}/approved-teams")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<TeamSelectionDTO>>> getApproveTeams(
            @PathVariable Integer eventId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        registrationEventService.getApprovedRegistrations(eventId),
                        "Danh sách team đã duyệt"
                )
        );
    }

    // Duyệt đăng ký
    @PatchMapping("/{registrationId}/approve")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Integer registrationId
    ) {

        registrationEventService.approveRegistration(registrationId);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã duyệt đơn đăng ký thành công")
        );
    }

    // Từ chối đăng ký
    @PatchMapping("/{registrationId}/reject")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Integer registrationId,
            @RequestParam String reason
    ) {

        registrationEventService.rejectRegistration(registrationId, reason);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Đã từ chối đơn đăng ký")
        );
    }


    // Lấy ra ds Team chờ duyệt
    @GetMapping("/{eventId}/pendingTeam")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> getTeamsForApproval(
            @PathVariable Integer eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        registrationEventService.getTeamsForApproval(eventId, userDetails),
                        "Danh sách chờ duyệt"
                )
        );
    }
    // Lấy ra ds Team chờ duyệt
    @GetMapping("/{eventId}/registration-all")
    public ResponseEntity<ApiResponse<List<TeamSelectionDTO>>> getALLRegistrations(
            @PathVariable Integer eventId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        registrationEventService.getAllTeamRegistrations(eventId),
                        "Danh sách đơn đăng kí thành công"
                )
        );
    }

    @GetMapping("/{registrationId}/pendingTeam-detail")
    public ResponseEntity<ApiResponse<RegistrationResponse>> getTeamsDetailForApproval(
            @PathVariable Integer registrationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        registrationEventService.getTeamsDetailForApproval(registrationId, userDetails),
                        "Chi tiết đăng ký"
                )
        );
    }

    @GetMapping("{eventId}/count-registration")
    public ResponseEntity<ApiResponse<CountRegistrationDTO>> getCountRegistration(@PathVariable Integer eventId){
        return ResponseEntity.ok(ApiResponse.success(registrationEventService.getCountRegistrations(eventId), ""));
    }


}
