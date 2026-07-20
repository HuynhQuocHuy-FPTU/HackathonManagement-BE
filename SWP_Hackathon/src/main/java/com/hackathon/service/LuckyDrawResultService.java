package com.hackathon.service;

import com.hackathon.dto.DrawResponseDTO;
import com.hackathon.dto.DrawResultRequestDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface LuckyDrawResultService {
    List<TeamParticipant> importDrawResults(Integer eventId, List<DrawResultRequestDTO> drawResults, CustomUserDetails userDetails, Integer responseDeadline);

    List<TeamParticipant> updateDrawResults(
            Integer eventId,
            List<DrawResultRequestDTO> drawResults,
            CustomUserDetails userDetails
    );

    List<DrawResponseDTO> getDrawResults(Integer eventId, CustomUserDetails userDetails);

}
