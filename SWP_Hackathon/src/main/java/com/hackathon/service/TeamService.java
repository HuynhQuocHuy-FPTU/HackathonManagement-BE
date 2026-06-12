package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.Account;
import com.hackathon.security.CustomUserDetails;

public interface TeamService {
    TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetails);

    TeamResponse updateInfo(CreateTeamRequest request, CustomUserDetails userDetails);

    void checkTeamRegistrationWindow();

    void leaveTeam(Integer teamId,CustomUserDetails userDetails);

    void acceptInvite(Integer teamId, Long notificationId,  CustomUserDetails userDetails);


}
