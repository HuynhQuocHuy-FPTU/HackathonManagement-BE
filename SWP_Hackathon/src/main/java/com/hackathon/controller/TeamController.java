package com.hackathon.controller;

import com.hackathon.dto.notification.NotificationEmailResponse;
import com.hackathon.dto.team.*;
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

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;

    private final NotificationService notificationService;
        /*
           1. NHÓM API QUẢN LÝ THÔNG TIN ĐỘI THI (STUDENT)
        */

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
                                                            @Valid @RequestBody TeamRequestDTO request,
                                                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.transferLeader(teamId, request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Gửi lời mời chuyển quyền Trưởng nhóm thành công!"));
    }
    /*
           2. NHÓM API XỬ LÝ LỜI MỜI / THÔNG BÁO (STUDENT)
    */


    //     View Invite
    @GetMapping("/notifications/{notiId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<NotificationEmailResponse>> getNotificationDetail(
            @PathVariable("notiId") Long notiId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        NotificationEmailResponse data = notificationService.getInfoNotificationInvite(userDetails, notiId);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thông tin lời mời thành công"));


    }

    // Accept invite
    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> acceptInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.acceptGeneralInvite(invitationId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Xử lý chấp nhận yêu cầu thành công!", "Hệ thống đã ghi nhận trạng thái mới."));
    }

    //  Reject Invite
    @PostMapping("/invitations/{invitationId}/reject")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> rejectInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.rejectGeneralInvite(invitationId, userDetails);
        return ResponseEntity.ok(ApiResponse.success("Xử lý từ chối yêu cầu thành công!", "Hệ thống đã ghi nhận trạng thái mới."));


    }

    // Send Invite
    @PostMapping("/invitations")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TeamResponse>> sendTeamInvitation(
            @Valid @RequestBody InviteTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        TeamResponse response = teamService.sendTeamInvitation(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Gửi lời mời vào nhóm thành công!"));
    }

    /*
          3. NHÓM API XEM THÀNH VIÊN ĐỘI (STUDENT / EXPERT / COORD / ADMIN)
   */
    // View Team of Student
    @GetMapping("/members/{teamId}")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamMembers(
            @PathVariable Integer teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamDetailResponse response = teamService.getTeamMember(teamId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem thành viên trong đội thành công"));
    }


    @GetMapping("/members/view-team-member-detail")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetailByStudent(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamDetailResponse response = teamService.getTeamDetailByStudentId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem thành viên chi tiết trong đội thành công"));
    }

    //View TeamDetail of Expert
    @GetMapping("/expert/detail/{teamId}")
    @PreAuthorize("hasAnyRole( 'EXPERT')")
    public ResponseEntity<ApiResponse<TeamDetailResponse>> getTeamDetail(
            @PathVariable Integer teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamDetailResponse response = teamService.getTeamDetail(teamId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem thành viên chi tiết trong đội do 1 expert quản lý thành công"));
    }

    // API dành riêng cho EXPERT - Xem team mình quản lý
    @GetMapping("/expert/my-member/{eventId}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getMyTeamInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId) {
        List<TeamDetailResponse> response = teamService.getTeamInfo( eventId,userDetails);
        if (response.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(response, "Bạn hiện chưa được phân công quản lý đội thi nào."));
        }
        return ResponseEntity.ok(ApiResponse.success(response, "Chuyên gia xem danh sách đội của mình thành công"));
    }


    // Vỉew Team of Admin
    @GetMapping("/admins/all")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getTeamForAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamDetailResponse> response = teamService.getTeamForAdmin(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin xem danh sách các team tham gia cuộc thi thành công"));
    }

    @GetMapping("/active")
    // @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TeamActiveResponse>>> getActiveTeams() {
        List<TeamActiveResponse> response = teamService.getActiveTeams();
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Lấy danh sách team đang hoạt động thành công"
        ));
    }

    @PostMapping("/{teamId}/join-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TeamJoinResponse>> sendJoinRequest(
            @PathVariable Integer teamId,
            @Valid @RequestBody TeamJoinRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        TeamJoinResponse response = teamService.sendJoinRequest(
                teamId,
                userDetails,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Gửi yêu cầu tham gia team thành công"
        ));
    }

    @GetMapping("/join-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TeamJoinResponse>>> getJoinRequestsForLeader(
            @RequestParam(defaultValue = "PENDING") com.hackathon.entity.enums.InvitationStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<TeamJoinResponse> response = teamService.getPendingJoinRequests(
                userDetails,
                status
        );
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Lấy danh sách yêu cầu tham gia team thành công"
        ));
    }

    @GetMapping("/join-requests/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TeamJoinResponse>>> getMyJoinRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<TeamJoinResponse> response =
                teamService.getTeamJoinRequestForMember(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Lấy danh sách yêu cầu đã gửi thành công"
        ));
    }

    @PutMapping("/join-requests/{requestId}/accept")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> acceptJoinRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        teamService.acceptJoinRequest(requestId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Chấp nhận yêu cầu tham gia team thành công"
        ));
    }

    @PutMapping("/join-requests/{requestId}/reject")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> rejectJoinRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        teamService.rejectJoinRequest(requestId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Từ chối yêu cầu tham gia team thành công"
        ));
    }


    // View Team of Leader về hạng mục thi ở từng vòng
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/category-round")
    public ResponseEntity<ApiResponse<TeamCompetitionResponse>> getTeamCompetition(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamCompetitionResponse response = teamService.getTeamCompetition(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Team Leader xem hạng mục thi đấu thành công."));
    }

}
