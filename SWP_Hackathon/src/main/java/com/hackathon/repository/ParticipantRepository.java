package com.hackathon.repository;

import com.hackathon.entity.TeamParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<TeamParticipation, Integer> {
    Optional<TeamParticipation> findParticipantByRegistration_RegistrationId(int registrationRegistrationId);
    List<TeamParticipation> findParticipantByCategoryRound_CategoryRoundId(Integer id);

    List<TeamParticipation> findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(int TeamId, int EventId);
}
