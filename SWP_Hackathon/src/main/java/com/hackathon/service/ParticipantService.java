package com.hackathon.service;

import com.hackathon.dto.ExpertAssignedGroupDTO;
import com.hackathon.entity.Participant;
import com.hackathon.entity.Registration;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ParticipantService {
    List<ExpertAssignedGroupDTO> getAssignParticipants(Integer eventId);
    public void disqualifyTeam(Integer eventId, Integer teamId, String reason);
    public Participant saveParticipant(Registration registration);



}
