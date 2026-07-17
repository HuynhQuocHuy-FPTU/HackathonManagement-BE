package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.ExpertRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {
    List<Evaluation> findBySubmission_SubmissionIdAndSubmission_IsFinal(int submissionSubmissionId, boolean submissionIsFinal);

    boolean existsBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(Integer categoryRoundId, EvaluationStatus status);

    List<Evaluation> findBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(Integer categoryRoundId, EvaluationStatus status);

    List<Evaluation> findBySubmission_TeamParticipant(TeamParticipant submissionTeamParticipant);

    @Query("SELECT e FROM Evaluation e "
            + "WHERE e.expertAssign.assignId = :assignId "
            + "AND e.submission.submissionId = :submissionId")
    Optional<Evaluation> findByExpertAssignIdAndSubmissionId(@Param("assignId") Integer assignId, @Param("submissionId") Integer submissionId);

    List<Evaluation> findBySubmission_SubmissionId(Integer submissionId);

    /**
     * ĐẾM
     */
    @Query("""
                SELECT COUNT(s)
                FROM Submission s
                JOIN s.teamParticipant tp
                JOIN tp.categoryRound cr
                JOIN cr.expertAssigns ea
                WHERE ea.expert.expertId = :expertId
                AND cr.round.hackathonEvent.eventId = :eventId
                AND ea.role IN :roles
                AND s.isFinal = true
            """)
    long countTotalAssigned(@Param("expertId") Integer expertId,
                            @Param("eventId") Integer eventId,
                            @Param("roles") List<com.hackathon.entity.enums.ExpertRole> roles);

    @Query("SELECT COUNT(e) " + "FROM Evaluation e " +
            "WHERE e.expertAssign.expert.expertId = :expertId " +
            "AND e.expertAssign.categoryRound.round.hackathonEvent.eventId = :eventId " +
            "AND e.status IN :statuses " + "AND e.expertAssign.role IN :roles")
    long countReviewsByStatuses(@Param("expertId") Integer expertId,
                                @Param("eventId") Integer eventId, @Param("statuses") List<EvaluationStatus> statuses,
                                @Param("roles") List<com.hackathon.entity.enums.ExpertRole> roles);
}