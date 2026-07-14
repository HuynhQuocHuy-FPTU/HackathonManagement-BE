package com.hackathon.repository;

import com.hackathon.entity.CategoryRound;
import com.hackathon.entity.Evaluation;
import com.hackathon.entity.TeamParticipant;
import com.hackathon.entity.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {
    List<Evaluation> findBySubmission_SubmissionIdAndStatus(int submissionSubmissionId, EvaluationStatus status);

    boolean existsBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(Integer categoryRoundId, EvaluationStatus status);

    List<Evaluation> findBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(Integer categoryRoundId, EvaluationStatus status);

    List<Evaluation> findBySubmission_TeamParticipant(TeamParticipant submissionTeamParticipant);

    @Query("SELECT e FROM Evaluation e " +
            "WHERE e.expertAssign.assignId = :assignId " +
            "AND e.submission.submissionId = :submissionId")
    Optional<Evaluation> findByExpertAssignIdAndSubmissionId(
            @Param("assignId") Integer assignId,
            @Param("submissionId") Integer submissionId);

    List<Evaluation> findBySubmission_SubmissionId(Integer submissionId);

    /**
     * ĐẾM
     */
    @Query("""
                SELECT COUNT(e)
                FROM Evaluation e
                WHERE e.expertAssign.expert.expertId = :expertId
                                  AND e.expertAssign.categoryRound.round.hackathonEvent.eventId = :eventId
                            
            """)
    long countTotalAssigned(@Param("expertId") Integer expertId, @Param("eventId") Integer eventId);

    @Query("SELECT COUNT (e) " +
            "FROM Evaluation e " +
            "WHERE e.expertAssign.expert.expertId = :expertId " +
            "AND e.expertAssign.categoryRound.round.hackathonEvent.eventId = :eventId " +
            "AND e.status IN('RE_EVALUATED', 'GRADED')")
    long countCompletedReviews(@Param("expertId") Integer expertId,@Param("eventId") Integer eventId);

    @Query("SELECT COUNT (e) " +
            "FROM Evaluation e " +
            "WHERE e.expertAssign.expert.expertId = :expertId " +
            "AND e.expertAssign.categoryRound.round.hackathonEvent.eventId = :eventId " +
            "AND e.status IN('NOT_GRADED', 'RE_EVALUATION')")

    long countPendingReviews(@Param("expertId") Integer expertId,@Param("eventId") Integer eventId);
    @Query("SELECT COUNT (e) " +
            "FROM Evaluation e " +
            "WHERE e.expertAssign.expert.expertId = :expertId " +
            "AND e.expertAssign.categoryRound.round.hackathonEvent.eventId = :eventId " +
            "AND e.status IN('RE_EVALUATION')")
    long reEvaluationReviews(@Param("expertId") Integer expertId,@Param("eventId") Integer eventId);

}