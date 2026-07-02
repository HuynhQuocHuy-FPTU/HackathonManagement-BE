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

    //    // View Team of ADMIN COORDINATOR(ĐỪNG XÓA PLEASE)
//    @GetMapping("/expert/{expertId}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EVENTCOORDINATOR')")
//    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getTeamInfo(
//            @PathVariable Integer expertId,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//        List<TeamDetailResponse> response = teamService.getTeamInfor(expertId, userDetails);
//        return ResponseEntity.ok(ApiResponse.success(response, "Xem danh sách thành viên trong đội do 1 expert quản lý thành công"));
//    }
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

//    // Vỉew Team of Admin
//    @GetMapping("/admin/members/{teamId}")
//    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getTeamForAdmin(
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//        List<TeamDetailResponse> response = teamService.getTeamForAdmin(userDetails);
//        return ResponseEntity.ok(ApiResponse.success(response, "Admin xem danh sách các team tham gia cuộc thi thành công"));
//    }

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
        return ResponseEntity.ok(ApiResponse.success(response, "Expert xem danh sách đội của mình thành công"));
    }


    // Vỉew Team of Admin
    @GetMapping("/admins/all")
    public ResponseEntity<ApiResponse<List<TeamDetailResponse>>> getTeamForAdmin(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamDetailResponse> response = teamService.getTeamForAdmin(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Admin xem danh sách các team tham gia cuộc thi thành công"));
    }


    // View Team of Leader về hạng mục thi ở từng vòng
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/category-round")
    public ResponseEntity<ApiResponse<TeamCompetitionResponse>> getTeamCompetition(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamCompetitionResponse response = teamService.getTeamCompetition(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Team Leader xem hạng mục thi đấu thành công."));
    }


    // Team gui request đến Mentor nhận sự hỗ trợ
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/team-request")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> teamSendRequestToMentor(
            @RequestParam String requestMessage,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamRequestResponse> response = teamService.teamSendRequestToMentor(requestMessage, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Team Leader gửi yêu cầu nhận sự hỗ trợ tới Mentor thành công."));
    }

    //Expert nhận list các Request mà Team gửi đến
    @PreAuthorize("hasRole('EXPERT')")
    @GetMapping("team-requests/received")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getTeamRequestsForExpert(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamRequestResponse> response = teamService.getTeamRequestsForExpert(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Expert nhận danh sách các yêu cầu nhận sự hỗ trợ thành công."));
    }

    //  Chấp nhận
    @PatchMapping("/team-requests/{requestId}/accept")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> acceptTeamRequest(
            @RequestParam String responseMessage,
            @PathVariable Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamRequestResponse response = teamService.acceptTeamRequest(responseMessage, requestId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Chấp nhận yêu cầu nhận hỗ trợ thành công."));
    }

    //  Từ chối
    @PatchMapping("/team-requests/{requestId}/reject")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> rejectTeamRequest(
            @RequestParam String responseMessage,
            @PathVariable Integer requestId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamRequestResponse response = teamService.rejectTeamRequest(responseMessage, requestId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Từ chối yêu cầu nhận hỗ trợ thành công."));
    }


}