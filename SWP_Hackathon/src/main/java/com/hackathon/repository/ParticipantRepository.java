package com.hackathon.repository;

import com.hackathon.entity.Participant;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Integer> {
    Optional<Participant> findParticipantByRegistration_RegistrationId(int registrationRegistrationId);
    List<Participant> findParticipantByCategoryRound_CategoryRoundId(Integer id);

    List<Participant> findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(int TeamId, int EventId);
}
