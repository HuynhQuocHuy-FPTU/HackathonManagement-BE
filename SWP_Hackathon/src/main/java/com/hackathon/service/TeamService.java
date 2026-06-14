package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.Account;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.security.CustomUserDetails;

public interface TeamService {
    HackathonEvent checkTeamRegistrationWindow(Integer eventId);

    TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetails);

    void updateInfo(CreateTeamRequest request, CustomUserDetails userDetails);

    void leaveTeam(TeamRequest request, CustomUserDetails userDetails);

    void transferLeader(TeamRequest request, CustomUserDetails userDetails);

    void acceptInvite(Integer teamId, Long notificationId, CustomUserDetails userDetails);


}
