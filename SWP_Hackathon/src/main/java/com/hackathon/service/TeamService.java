package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamResponse;

public interface TeamService {
    TeamResponse createTeam(CreateTeamRequest request);

    TeamResponse updateInfo(CreateTeamRequest request);

    String getCurrentUserEmail();

    void checkTeamRegistrationWindow();

    void leaveTeam(Integer teamId);

    void acceptInvite(Integer teamId, Long notificationId);
}
