package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.security.CustomUserDetails;

import java.math.BigDecimal;
import java.util.List;


public interface ParticipantService {
    List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId, CustomUserDetails userDetails);

    void disqualifyTeam(Integer eventId, Integer teamId, String reason);

    TeamParticipant saveParticipant(Registration registration);

    BigDecimal calculateTotalScore(TeamParticipant participant);

    BigDecimal calculateTotalScore(Integer teamParticipantId);


}
