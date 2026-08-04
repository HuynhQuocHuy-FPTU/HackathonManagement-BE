package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Team;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.Registration;
import com.hackathon.entity.enums.ParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    List<TeamParticipant> findAllByRegistration_HackathonEvent_EventIdAndCategoryRoundIsNotNull(
            Integer eventId
    );

    boolean existsByRegistration_Team_TeamIdInAndCategoryRound_Round_RoundId(Collection<Integer> registrationTeamTeamIds, Integer categoryRoundRoundRoundId);

    @Query("""
            SELECT tp
            FROM TeamParticipant tp
            WHERE tp.categoryRound.round.roundId = :roundId
            ORDER BY tp.rank ASC
            """)
    List<TeamParticipant> findByRoundId(Integer roundId);
    
    @Query("SELECT tp FROM TeamParticipant tp " +
            "JOIN tp.registration r " +
            "JOIN r.team t " +
            "WHERE t.teamId = :teamId " +
            "AND tp.categoryRound.round.roundId = :roundId " +
            "AND r.status = RegistrationStatus.APPROVED " +
            "AND tp.status = ParticipantStatus.RE_EVALUATING")
    Optional<TeamParticipant> findReEvaluatingParticipant(
            @Param("teamId") Integer teamId,
            @Param("roundId") Integer roundId);

    @Query("SELECT tp FROM TeamParticipant tp " +
            "JOIN tp.registration r " +
            "JOIN r.team t " +
            "WHERE t.teamId = :teamId " +
            "AND r.status = RegistrationStatus.APPROVED " +
            "AND tp.status = ParticipantStatus.ACTIVE"
    )
    TeamParticipant findByTeamIdForSubmission(@Param("teamId") Integer teamId);



    @Query("""
    SELECT t
    FROM TeamParticipant tp
    JOIN tp.registration r
    JOIN r.team t
    WHERE tp.status = :status
    """)
    List<Team> getTeamsByParticipantStatus(@Param("status") ParticipantStatus status);
}
