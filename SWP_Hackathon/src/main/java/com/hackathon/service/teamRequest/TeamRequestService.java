package com.hackathon.service.teamRequest;


import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.dto.team.ProcessTeamRequest;
import com.hackathon.dto.team.CreateDirectTeamRequest;
import com.hackathon.dto.notification.NotiResponseRequest;
import com.hackathon.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeamRequestService {
    List<TeamRequestResponse> teamSendRequestToMentor(TeamAppealRequestDTO request, CustomUserDetails userDetails);

    Page<TeamRequestResponse> getTeamRequestsForExpert(
            CustomUserDetails userDetails, Pageable pageable);

    TeamRequestResponse acceptTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails);

    TeamRequestResponse rejectTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails);

    Page<TeamRequestResponse> getAppealRequest(
            CustomUserDetails userDetails, Integer roundId, Pageable pageable);

    Page<TeamRequestResponse> getAppealRequestPublic(
            CustomUserDetails userDetails, Integer roundId, Pageable pageable);

    Page<TeamRequestResponse> getAppealRequestsForJudge(
            CustomUserDetails userDetails, Integer roundId, Pageable pageable);

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
