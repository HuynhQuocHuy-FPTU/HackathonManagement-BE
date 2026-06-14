package com.hackathon.controller;

import com.hackathon.dto.event.EventResponse;
import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.EventService;
import com.hackathon.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/registration")
public class RegistrationController {
    @Autowired
    private EventService eventService;
    @Autowired
    private TeamService teamService;

    //View General Information about hackathon
    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvent() {
        List<EventResponse> list = eventService.getAllEvent();
        return ResponseEntity.ok(ApiResponse.success(list, "Get all events successfully"));
    }


    //View all information detail about hackathon(click Event show details)
    @GetMapping("/events/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable Integer eventId) {
        EventResponse list = eventService.getEventDetail(eventId);
        return ResponseEntity.ok(ApiResponse.success(list, "Get all event details successfully"));
    }

    //Search HackathonEvent By EventName
    @GetMapping("/events/search")
    public ResponseEntity<ApiResponse<List<EventResponse>>> searchHackathonEvent(@RequestParam("eventName") String name) {
        List<EventResponse> list = eventService.searchByEventName(name);
        return ResponseEntity.ok(ApiResponse.success(list, "Search Successfully"));
    }

    //Create Team
    @PostMapping("/events/{eventId}/teams")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@PathVariable Integer eventId,@RequestBody CreateTeamRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        request.setEventId(eventId);
        TeamResponse team = teamService.createTeam(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(team, "Create Team successfully"));
    }

    //Update infor Team
    @PutMapping("/events/{eventId}/teams")
    public ResponseEntity<ApiResponse<Void>> updateTeam(@PathVariable Integer eventId,@RequestBody CreateTeamRequest request,@AuthenticationPrincipal CustomUserDetails userDetails) {
        request.setEventId(eventId);
        teamService.updateInfo(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Update Team Successfully"));
    }

//    // Out Team
//    @PostMapping("/teams/{teamId}/leave")
//    public ResponseEntity<ApiResponse<Void>> leaveTeam(@PathVariable Integer teamId, @AuthenticationPrincipal CustomUserDetails userDetails) {
//        teamService.leaveTeam(teamId, userDetails);
//        return ResponseEntity.ok(ApiResponse.success(null, "Leave Team Successfully"));
//    }

    //Accept Invite
    @PostMapping("/teams/{teamId}/accept-invite/{notiId}")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(
            @PathVariable Integer teamId, @PathVariable Long notiId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        teamService.acceptInvite(teamId, notiId, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Invitation accepted successfully"
                )
        );
    }
}
