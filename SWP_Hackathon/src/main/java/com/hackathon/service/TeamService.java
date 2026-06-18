package com.hackathon.service;

import com.hackathon.dto.team.CreateTeamRequest;
import com.hackathon.dto.team.TeamRequest;
import com.hackathon.dto.team.TeamResponse;
import com.hackathon.entity.HackathonEvent;
import com.hackathon.entity.Notification;
import com.hackathon.entity.Team;
import com.hackathon.security.CustomUserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

public interface TeamService {
    //    HackathonEvent checkEventRegistrationWindow(Integer eventId);
    void checkEventRegistrationWindow(Team team);

    TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetails);

    TeamResponse  sendTeamInvitation(CreateTeamRequest request,CustomUserDetails userDetails);

    String updateInfo(CustomUserDetails userDetails, String teamName);

    void acceptInvite(Notification notification, CustomUserDetails userDetails);

    void leaveTeam(CustomUserDetails userDetails, Integer teamId);

    void transferLeader(Integer teamId, TeamRequest request, CustomUserDetails userDetails);

    void acceptGeneralInvite(Long notificationId, CustomUserDetails userDetails);

    void acceptLeaderTransfer(Notification notification, CustomUserDetails userDetails);

    void rejectGeneralInvite(Long notificationId, CustomUserDetails userDetails);

    void rejectLeaderTransferInvite(Notification notification, CustomUserDetails userDetails);

    void rejectTeamInvite(Notification notification, CustomUserDetails userDetails);

    TeamResponse getTeamMember(Integer teamId, CustomUserDetails userDetails);

    void registerEvent(Integer eventId, CustomUserDetails userDetails);

}
