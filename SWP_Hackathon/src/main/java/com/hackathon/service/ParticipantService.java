package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.entity.TeamParticipation;
import com.hackathon.entity.Registration;
import com.hackathon.security.CustomUserDetails;

import java.util.List;


public interface ParticipantService {
    List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId, CustomUserDetails userDetails);
    public void disqualifyTeam(Integer eventId, Integer teamId, String reason);
    public TeamParticipation saveParticipant(Registration registration);
}
