package com.hackathon.service;

import com.hackathon.entity.Participant;
import com.hackathon.entity.Registration;
import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.repository.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipantService {
    private final ParticipantRepository participantRepository;

    public Participant saveParticipant(Registration registration){
        Participant participant = new Participant();
        participant.setRegistration(registration);
        participant.setCategoryRound(null);
        participant.setStatus(ParticipantStatus.ACTIVE);

        return participantRepository.save(participant);
    }


}
