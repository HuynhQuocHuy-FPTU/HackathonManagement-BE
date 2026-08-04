package com.hackathon.service;

import com.hackathon.dto.team.*;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamInvitation;
import com.hackathon.entity.enums.InvitationStatus;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface TeamService {
    void checkEventRegistrationWindow(Team team);

    TeamResponse createTeam(CreateTeamRequest request, CustomUserDetails userDetails);

    TeamResponse sendTeamInvitation(InviteTeamRequest request, CustomUserDetails userDetails);

    String updateInfo(CustomUserDetails userDetails, String teamName);

    void acceptTeamDraftInvite(TeamInvitation invitation, CustomUserDetails userDetails);

    void acceptOfficialInvite(TeamInvitation invitation, CustomUserDetails userDetails);

    void leaveTeam(CustomUserDetails userDetails, Integer teamId);

    void transferLeader(Integer teamId, TeamRequestDTO request, CustomUserDetails userDetails);

    void acceptGeneralInvite(Long notificationId, CustomUserDetails userDetails);

    void acceptLeaderTransfer(TeamInvitation invitation, CustomUserDetails userDetails);

    void rejectGeneralInvite(Long notificationId, CustomUserDetails userDetails);

    void rejectLeaderTransferInvite(TeamInvitation invitation, CustomUserDetails userDetails);

    void rejectTeamInvite(TeamInvitation teamInvitation,CustomUserDetails userDetails);

    TeamDetailResponse getTeamMember(Integer teamId, CustomUserDetails userDetails);

    List<TeamDetailResponse> getTeamForAdmin(CustomUserDetails userDetails);

    TeamDetailResponse getTeamDetail(Integer teamId, CustomUserDetails userDetails);

    List<TeamDetailResponse> getTeamInfo(Integer eventId, CustomUserDetails userDetails);

    TeamDetailResponse getTeamDetailByStudentId(CustomUserDetails userDetails);

    TeamCompetitionResponse getTeamCompetition(CustomUserDetails userDetails);

    List<TeamActiveResponse> getActiveTeams();

    TeamJoinResponse sendJoinRequest(
            Integer teamId,
            CustomUserDetails userDetails,
            TeamJoinRequest request
    );

    List<TeamJoinResponse> getPendingJoinRequests(
            CustomUserDetails userDetails,
            InvitationStatus status
    );

    List<TeamJoinResponse> getTeamJoinRequestForMember(CustomUserDetails userDetails);

    void acceptJoinRequest(
            Long requestId,
            CustomUserDetails userDetails
    );

    void rejectJoinRequest(
            Long requestId,
            CustomUserDetails userDetails
    );



}
