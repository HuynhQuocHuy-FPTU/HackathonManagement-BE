package com.hackathon.controller;

import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.dto.team.ProcessTeamRequest;
import com.hackathon.dto.team.CreateDirectTeamRequest;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.TeamRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;


@RestController
@RequestMapping("/api/team-request")
@RequiredArgsConstructor
public class TeamRequestController {
    private final TeamRequestService teamRequestService;

    @PostMapping("/direct")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> createDirectRequest(
            @Valid @RequestBody CreateDirectTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamRequestResponse response =
                teamRequestService.createDirectRequest(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success(
                response, "Tạo yêu cầu trực tiếp thành công"));
    }

    @PatchMapping("/{requestId}/process")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> processRequest(
            @PathVariable Integer requestId,
            @Valid @RequestBody ProcessTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamRequestResponse response = teamRequestService.processRequest(
                userDetails, requestId, request);
        return ResponseEntity.ok(ApiResponse.success(
                response, "Xử lý yêu cầu thành công"));
    }

    // Team gui request đến Mentor nhận sự hỗ trợ
    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> teamSendRequestToMentor(
            @RequestBody TeamAppealRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamRequestResponse> response = teamRequestService.teamSendRequestToMentor(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Team Leader gửi yêu cầu nhận sự hỗ trợ tới Mentor thành công."));
    }

    //Expert nhận list các Request mà Team gửi đến
    @PreAuthorize("hasRole('EXPERT')")
    @GetMapping("/received/{roundId}")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getTeamRequestsForExpert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer roundId) {
        List<TeamRequestResponse> response =
                teamRequestService.getTeamRequestsForExpert(roundId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Chuyên gia nhận danh sách các yêu cầu nhận sự hỗ trợ thành công."));
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

    //Ban tổ chức lấy toàn bộ danh sách đơn khiếu nại
    @GetMapping("/appeal/{roundId}")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getAppealRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer roundId) {
        List<TeamRequestResponse> response =
                teamRequestService.getAppealRequest(
                        userDetails, roundId);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức lấy toàn bộ danh sách đơn phúc khảo thành công."));
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getAllRequestsForEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId
    ) {
        List<TeamRequestResponse> response =
                teamRequestService.getAllRequestsForEvent(
                        userDetails, eventId);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Lấy toàn bộ yêu cầu của sự kiện thành công"));
    }

    @GetMapping("/applicaion/view-all/{roundId}")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getAppealRequestPublic(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer roundId) {
        List<TeamRequestResponse> response =
                teamRequestService.getAppealRequestPublic(
                        userDetails, roundId);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức lấy toàn bộ danh sách đơn phúc khảo thành công."));
    }


    // STUDENT XEM DS ĐƠN ĐÃ GỬI YÊU CẦU ĐẾN MENTOR
    @GetMapping("/{eventId}/mentor/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getMyMentorSupportRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId) {
        List<TeamRequestResponse> response =
                teamRequestService.getMyMentorSupportRequests(
                        userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sinh viên xem các yêu cầu đã gửi đến MENTOR thành công."));
    }

    // STUDNET XEM DS ĐƠN ĐÃ GỬI YÊU CẦU KHIẾU NẠI
    @GetMapping("/{eventId}/appeals/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getMyAppealRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer eventId) {
        List<TeamRequestResponse> response =
                teamRequestService.getMyAppealRequests(
                        userDetails, eventId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sinh viên xem các yêu cầu đã gửi khiếu nại thành công."));
    }



}
