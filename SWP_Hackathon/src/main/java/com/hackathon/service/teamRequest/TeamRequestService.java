package com.hackathon.service.teamRequest;


import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.dto.team.ProcessTeamRequest;
import com.hackathon.dto.team.CreateDirectTeamRequest;
import com.hackathon.dto.notification.NotiResponseRequest;
import com.hackathon.security.CustomUserDetails;
import java.util.List;

public interface TeamRequestService {
    List<TeamRequestResponse> teamSendRequestToMentor(TeamAppealRequestDTO request, CustomUserDetails userDetails);

    List<TeamRequestResponse> getTeamRequestsForExpert(
            CustomUserDetails userDetails);

    TeamRequestResponse acceptTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails);

    TeamRequestResponse rejectTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails);

    List<TeamRequestResponse> getAppealRequest(
            CustomUserDetails userDetails, Integer roundId);

    List<TeamRequestResponse> getAllRequestsForEvent(
            CustomUserDetails userDetails, Integer eventId);

    List<TeamRequestResponse> getAppealRequestPublic(
            CustomUserDetails userDetails, Integer roundId);

    List<TeamRequestResponse> getAppealRequestsForJudge(
            CustomUserDetails userDetails, Integer roundId);

    // Đêr tạm ơr đây
//    void reEvaluationSubmission(CustomUserDetails userDetails, ReDetailEvaluationRequest request );

    TeamRequestResponse respondNotification(
            CustomUserDetails userDetails,
            Long notificationId,
            NotiResponseRequest request
    );

    TeamRequestResponse processRequest(CustomUserDetails userDetails, Integer requestId,
                                       ProcessTeamRequest request);

    TeamRequestResponse createDirectRequest(
            CustomUserDetails userDetails,
            CreateDirectTeamRequest request
    );
}
