package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.entity.enums.ParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<TeamParticipant, Integer> {
    Optional<TeamParticipant> findParticipantByRegistration_RegistrationId(int registrationRegistrationId);

    List<TeamParticipant> findParticipantByCategoryRound_CategoryRoundId(Integer id);

    List<TeamParticipant> findParticipantByRegistration_Team_TeamIdAndRegistration_HackathonEvent_EventId(int TeamId, int EventId);

    Optional<TeamParticipant> findTeamParticipantByRegistration_RegistrationIdAndStatus(int registrationRegistrationId, ParticipantStatus status);

    List<TeamParticipant> findByCategoryRound_CategoryRoundIdAndStatusIsNotIn(int categoryRoundCategoryRoundId, Collection<ParticipantStatus> statuses);

    boolean existsByCategoryRound_CategoryRoundIdAndRegistration_RegistrationId(int categoryRoundCategoryRoundId, int registrationRegistrationId);

    List<TeamParticipant> findByCategoryRound_CategoryRoundId(int categoryRoundId);

    boolean existsByRegistration_Team_TeamIdInAndCategoryRound_Round_RoundId(Collection<Integer> registrationTeamTeamIds, Integer categoryRoundRoundRoundId);

    @Query("""
            SELECT tp
            FROM TeamParticipant tp
            WHERE tp.categoryRound.round.roundId = :roundId
            """)
    List<TeamParticipant> findByRoundId(Integer roundId);
}
