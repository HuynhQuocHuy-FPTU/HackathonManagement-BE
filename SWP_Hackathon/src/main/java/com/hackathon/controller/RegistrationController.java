package com.hackathon.controller;

import com.hackathon.dto.TeamSelectionDTO;
import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.notification.NotificationResponse;
import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamDetailResponse;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.Registration;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.NotificationService;
import com.hackathon.service.RegistrationEventService;
import com.hackathon.service.TeamService;
import com.hackathon.service.event.EventService;
import jakarta.persistence.criteria.CriteriaBuilder;
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
    private TeamService teamService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private RegistrationEventService registrationEventService;

//    //View General Information about hackathon
//    @GetMapping("/events")
//    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvent() {
//        List<EventResponse> list = eventService.getAllEvent();
//        return ResponseEntity.ok(ApiResponse.success(list, "Get all events thành công"));
//    }
//
//
//    //View all information detail about hackathon(click Event show details)
//    @GetMapping("/events/{eventId}")
//    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable Integer eventId) {
//        EventResponse list = eventService.getEventDetail(eventId);
//        return ResponseEntity.ok(ApiResponse.success(list, "Get all event details thành công"));
//    }
//
//    //Search HackathonEvent By EventName
//    @GetMapping("/events/search")
//    public ResponseEntity<ApiResponse<List<EventResponse>>> searchHackathonEvent(@RequestParam("eventName") String name) {
//        List<EventResponse> list = eventService.searchByEventName(name);
//        if (list.isEmpty()) {
//            return ResponseEntity.ok(
//                    ApiResponse.success(list, "Không tìm thấy sự kiện phù hợp")
//            );
//        }
//
//        return ResponseEntity.ok(
//                ApiResponse.success(list, "Tìm kiếm thành công")
//        );
//    }

    @GetMapping("/{eventId}/approveTeam-detail")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsForApproval(@PathVariable Integer eventId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<TeamResponse> list = registrationEventService.getTeamsForApproval(eventId,userDetails);
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
    @PostMapping("/teams/invite")
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

    // Lấy ra list team đã được approve
    @GetMapping("/{eventId}/approved-teams")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ResponseEntity<List<TeamSelectionDTO>> getApproveTeams(@PathVariable Integer eventId){
        List<TeamSelectionDTO> teams = registrationEventService.getApprovedRegistrations(eventId);
        return ResponseEntity.ok(teams);
    }

    // Duyệt đăng ký
    @PatchMapping("/{registrationId}/approve")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ApiResponse<Registration> approve(@PathVariable Integer registrationId) {
        Registration registration = registrationEventService.approveRegistration(registrationId);
        return ApiResponse.success(registration, "Đã duyệt đơn đăng ký thành công");
    }

    // Từ chối đăng ký
    @PatchMapping("/{registrationId}/reject")
    @PreAuthorize("hasRole('EVENTCOORDINATOR')")
    public ApiResponse<Registration> reject(@PathVariable Integer registrationId) {
        Registration registration = registrationEventService.rejectRegistration(registrationId);
        return ApiResponse.success(registration, "Đã từ chối đơn đăng ký");
    }




}