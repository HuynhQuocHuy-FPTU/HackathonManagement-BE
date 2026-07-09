package com.hackathon.repository;

import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.entity.enums.ParticipantStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<TeamParticipant, Integer> {
    Optional<TeamParticipant> findParticipantByRegistration_RegistrationId(int registrationRegistrationId);

    List<TeamParticipant> findParticipantByCategoryRound_CategoryRoundId(Integer id);

    List<TeamParticipant> findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(int TeamId, int EventId);

    Optional<TeamParticipant> findTeamParticipantByRegistration_RegistrationIdAndStatus(int registrationRegistrationId, ParticipantStatus status);

    List<TeamParticipant> findByCategoryRound_CategoryRoundIdAndStatus(int categoryRoundCategoryRoundId, ParticipantStatus status);
    List<TeamParticipant> findByCategoryRound_CategoryRoundIdAndStatus(int categoryRoundCategoryRoundId, ParticipantStatus status,  Pageable pageable);

    boolean existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(int categoryRoundCategoryRoundId, int registrationRegistrationId);


}
