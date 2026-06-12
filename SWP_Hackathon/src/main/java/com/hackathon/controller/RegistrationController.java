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
    @GetMapping("/event")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvent() {
        List<EventResponse> list = eventService.getAllEvent();
        return ResponseEntity.ok(ApiResponse.success(list, "Get all events successfully"));
    }


    //View all information detail about hackathon(click Event show details)
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventDetail(@PathVariable("eventId") Integer id) {
        EventResponse list = eventService.getEventDetail(id);
        return ResponseEntity.ok(ApiResponse.success(list, "Get all event details successfully"));
    }

    //Search HackathonEvent By EventName
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EventResponse>>> searchHackathonEvent(@RequestParam("eventName") String name) {
        List<EventResponse> list = eventService.searchByEventName(name);
        return ResponseEntity.ok(ApiResponse.success(list, "Search Successfully"));
    }

    //Create Team
    @PostMapping("/createTeam")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@RequestBody CreateTeamRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamResponse team = teamService.createTeam(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(team, "Create Team successfully"));
    }

    //Update infor Team
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(@RequestBody CreateTeamRequest request,
                                                                @AuthenticationPrincipal CustomUserDetails userDetails) {
        TeamResponse team = teamService.updateInfo(request, userDetails);
        return ResponseEntity.ok(ApiResponse.success(team, "Update Team Successfully"));
    }

    // Out Team
    @PostMapping("/{teamId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveTeam(@PathVariable("teamId") Integer teamId,@AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.leaveTeam(teamId, userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "Leave Team Successfully"));
    }

    //Accept Invite
    @PostMapping("/{id}/accept-invite/{notiId}")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(@PathVariable("id") Integer teamId,  @PathVariable ("notiId") Long notificationId,@AuthenticationPrincipal CustomUserDetails userDetails){
        teamService.acceptInvite(teamId,notificationId, userDetails);
        return ResponseEntity.ok(
                ApiResponse.success(null,"Invitation accepted successfully"
                )
        );
    }
}
