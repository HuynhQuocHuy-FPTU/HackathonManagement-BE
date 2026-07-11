package com.hackathon.service.teamRequest;


import com.hackathon.dto.TeamAppealRequestDTO;
import com.hackathon.dto.evaluation.ReDetailEvaluationRequest;
import com.hackathon.dto.team.TeamRequestResponse;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface TeamRequestService {
    List<TeamRequestResponse> teamSendRequestToMentor(String requestMessage, CustomUserDetails userDetails);

    List<TeamRequestResponse> getTeamRequestsForExpert(CustomUserDetails userDetails);

    TeamRequestResponse acceptTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails);

    TeamRequestResponse rejectTeamRequest(String responseMessage, Integer requestId, CustomUserDetails userDetails);

    List<TeamRequestResponse> teamSendAppealRequest(TeamAppealRequestDTO request, CustomUserDetails userDetails);

    List<TeamRequestResponse> getAppealRequest(CustomUserDetails userDetails, Integer roundId);

    TeamRequestResponse rejectAppealRequest(CustomUserDetails userDetails, Integer requestId,String responseMessage);

    TeamRequestResponse acceptAppealRequest(CustomUserDetails userDetails, Integer requestId,String responseMessage);

    TeamRequestResponse requestExpertToReEvaluation(CustomUserDetails userDetails, Integer requestId);

    List<TeamRequestResponse> getAppealRequestsForJudge(CustomUserDetails userDetails, Integer roundId);

    // Đêr tạm ơr đây
//    void reEvaluationSubmission(CustomUserDetails userDetails, ReDetailEvaluationRequest request );

    TeamRequestResponse sendRequestToCoordinator(CustomUserDetails userDetails, String requestMessage, Long notificationId);
    void resolvedRequest(CustomUserDetails userDetails, Integer teamRequestId, String messageResponse);
}
