package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.security.CustomUserDetails;

public interface TeamService {
    HackathonEvent checkTeamRegistrationWindow(Integer eventId);

    TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetails);

    String updateInfo(CustomUserDetails userDetails, String teamName);

    void transferLeader(TeamRequest request, CustomUserDetails userDetails);

    void acceptInvite(Integer teamId, Long notificationId, CustomUserDetails userDetails);

    void leaveTeam(CustomUserDetails userDetails);


}
