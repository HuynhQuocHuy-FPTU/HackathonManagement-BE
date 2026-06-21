package com.hackathon.controller;

import com.hackathon.dto.notification.NotificationResponse;
import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    private final NotificationService notificationService;

    //Create Team
    @PostMapping("/create")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody CreateTeamRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamResponse team = teamService.createTeam(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(team, "Tạo Team thành công"));
    }

    //Update infor Team
    @PreAuthorize("hasRole('STUDENT')")
    @PutMapping("/update/teams-name")
    public ResponseEntity<ApiResponse<String>> updateTeam(@RequestParam String teamName, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String name = teamService.updateInfo(userDetails, teamName);
        return ResponseEntity.ok(ApiResponse.success(teamName, "Cập nhật thông tim Team thành công"));
    }

    // Out Team
    @PostMapping("/{teamId}/leave")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(@PathVariable Integer teamId,
                                                       @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.leaveTeam(userDetails, teamId);
        return ResponseEntity.ok(ApiResponse.success(null, "Rời Team thành công"));
    }

    // Transfer Leader
    @PutMapping("/{teamId}/transfer-leader")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> transferLeader(@PathVariable Integer teamId,
                                                            @Valid @RequestBody TeamRequest request ,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.transferLeader(teamId,request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Gửi lời mời chuyển quyền Trưởng nhóm thành công!"));
    }

    //     View Invite
    @GetMapping("/notifications/{notiId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<NotificationResponse>> getNotificationDetail(
            @PathVariable("notiId") Long notiId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        NotificationResponse data = notificationService.getInfoNotificationInvite(userDetails, notiId);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thông tin lời mời thành công"));


    }

    // Accept invite
    @PostMapping("/notifications/{notiId}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> acceptInvitation(
            @PathVariable Long notiId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.acceptGeneralInvite(notiId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Xử lý chấp nhận yêu cầu thành công!", "Hệ thống đã ghi nhận trạng thái mới."));
    }
    //  Reject Invite
    @PostMapping("/notifications/{notiId}/reject")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> rejectInvitation(
            @PathVariable Long notiId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.rejectGeneralInvite(notiId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Xử lý từ chối yêu cầu thành công!", "Hệ thống đã ghi nhận trạng thái mới."));


    }

    // View Team
    @GetMapping("/members/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamMembers(
            @PathVariable Integer teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TeamDetailResponse response = teamService.getTeamMember(teamId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response,"Xem thành viên trong đội thành công"));
    }


}