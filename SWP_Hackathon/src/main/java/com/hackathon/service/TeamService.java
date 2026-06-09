package com.hackathon.service;

import com.hackathon.dto.Team.request.CreateTeamRequest;
import com.hackathon.dto.Team.response.TeamResponse;

public interface TeamService {
    TeamResponse createTeam (CreateTeamRequest request);
    void updateInfo(CreateTeamRequest request, Integer teamId, Integer accId);
}
