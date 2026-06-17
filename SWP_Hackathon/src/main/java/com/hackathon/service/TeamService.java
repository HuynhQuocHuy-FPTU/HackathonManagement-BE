package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Notification;
import com.hackathon.security.CustomUserDetails;
import org.springframework.stereotype.Service;
public interface TeamService {
    HackathonEvent checkTeamRegistrationWindow(Integer eventId);

    TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetails);

    String updateInfo(CustomUserDetails userDetails, String teamName);

    void acceptInvite(Notification notification, CustomUserDetails userDetails);

    void leaveTeam(CustomUserDetails userDetails);

    void transferLeader(TeamRequest request, CustomUserDetails userDetails);

    void acceptGeneralInvite(Long notificationId, CustomUserDetails userDetails);

    void acceptLeaderTransfer(Notification notification,CustomUserDetails userDetails);


}
