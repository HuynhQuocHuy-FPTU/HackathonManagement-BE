package com.hackathon.service;

import com.hackathon.dto.participant.ExpertAssignedGroupDTO;
import com.hackathon.dto.participant.CurrentParticipantDTO;
import com.hackathon.dto.participant.CategoryParticipantDTO;
import com.hackathon.dto.participant.RoundParticipantDetailDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.security.CustomUserDetails;

import java.util.List;


public interface ParticipantService {
    List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId, CustomUserDetails userDetails);

    void disqualifyTeam(Integer eventId, Integer teamId, String reason);

    TeamParticipant saveParticipant(Registration registration);

    CurrentParticipantDTO getCurrentParticipant(CustomUserDetails userDetails);

    RoundParticipantDetailDTO getDetailParticipantByRound(Integer roundId, CustomUserDetails userDetails);
}
