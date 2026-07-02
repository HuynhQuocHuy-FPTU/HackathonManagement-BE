package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.dto.ParticipantResponseDTO;
import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.RankingResponseDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.repository.ParticipantRepository;
import com.hackathon.security.CustomUserDetails;

import java.util.List;


public interface ParticipantService {
    List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId, CustomUserDetails userDetails);

    void disqualifyTeam(Integer eventId, Integer teamId, String reason);

    TeamParticipant saveParticipant(Registration registration);

    CategoryRoundRankingResponse getRankingByEventCoordinator(Integer roundId, CustomUserDetails userDetails);

    CategoryRoundRankingResponse approveRanking(CustomUserDetails userDetails, Integer roundId);

    CategoryRoundRankingResponse rejectRanking(CustomUserDetails userDetails, Integer roundId);

    void publishDraftRanking(Integer roundId, CustomUserDetails userDetails);

    void publishFinalRanking(Integer roundId, CustomUserDetails userDetails);

}
