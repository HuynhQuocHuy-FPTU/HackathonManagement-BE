package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamResponse;

public interface TeamService {
    TeamResponse createTeam (CreateTeamRequest request);
    void updateInfo(CreateTeamRequest request, Integer teamId, Integer accId);
}
