package com.hackathon.controller;

import com.hackathon.dto.TeamAppealRequestDTO;
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
    @PostMapping
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

    //-------------------------
    // TEAM GỬI KHIẾU NẠI
    //---------------------------


    // 1. TEM LEADER gửi khiếu nại về round
    @PostMapping("/appeal")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> teamSendAppealRequest(
            @RequestBody TeamAppealRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamRequestResponse> response = teamRequestService.teamSendAppealRequest(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Đội thi gửi yêu cầu phúc khảo thành công."));
    }

    //Ban tổ chức lấy toàn bộ danh sách đơn khiếu nại
    @GetMapping("/appeal/{roundId}")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getAppealRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer roundId) {
        List<TeamRequestResponse> response = teamRequestService.getAppealRequest(userDetails, roundId);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức lấy toàn bộ danh sách đơn phúc khảo thành công."));
    }

    // từ chối đơn khiếu nại khi ko có sự thay đỏi dì
    @PatchMapping("/appeal/{requestId}/reject")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> rejectAppealRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer requestId,
            @RequestParam(required = false) String responseMessage) {
        TeamRequestResponse response = teamRequestService.rejectAppealRequest(userDetails, requestId, responseMessage);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức từ chối đơn phúc khảo thành công."));
    }

    // chấp nhận đơn khiếu nại khi  có sự thay đỏi
    @PatchMapping("/appeal/{requestId}/accept")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> acceptAppealRequest(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer requestId,
            @RequestParam(required = false) String responseMessage) {
        TeamRequestResponse response = teamRequestService.acceptAppealRequest(userDetails, requestId, responseMessage);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức chấp nhận đơn phúc khảo thành công."));
    }


    //Ban tổ chức chuyển tiếp đơn khiếu nại, yêu cầu Giám khảo đã chấm tiến hành phúc khảo lại bài nộp.
    @PatchMapping("/appeal/{requestId}/forward-to-judge")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<TeamRequestResponse>> requestExpertToReEvaluation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer requestId) {
        TeamRequestResponse response = teamRequestService.requestExpertToReEvaluation(userDetails, requestId);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban tổ chức gửi yêu cầu đến Ban giám khảo chấm lại bài dự thi thành công."));
    }

    // BAN GIÁM KHẢO NHẬN DS BÀI NỘP ĐỂ TIẾN HÀNH CHẤM LẠI
    @GetMapping("/appeal/{roundId}/review-submissions")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<ApiResponse<List<TeamRequestResponse>>> getAppealRequestsForJudge(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Integer roundId) {
        List<TeamRequestResponse> response = teamRequestService.getAppealRequestsForJudge(userDetails, roundId);
        return ResponseEntity.ok(ApiResponse.success(response, "Ban giám khảo nhận các bài nộp yêu cầu phúc khảo thành công."));
    }

}
