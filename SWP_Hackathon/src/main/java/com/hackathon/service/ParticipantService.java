package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;

import java.util.List;


public interface ParticipantService {
    List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId);
    public void disqualifyTeam(Integer eventId, Integer teamId, String reason);
    public TeamParticipant saveParticipant(Registration registration);



}
