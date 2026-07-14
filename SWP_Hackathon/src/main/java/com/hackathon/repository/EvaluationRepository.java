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


}