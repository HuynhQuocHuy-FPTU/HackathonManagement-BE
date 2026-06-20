package com.hackathon.repository;

import com.hackathon.entity.Participant;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Integer> {
    Optional<Participant> findParticipantByRegistration_RegistrationId(int registrationRegistrationId);
}
