package com.hackathon.repository;

import com.hackathon.entity.Evaluation;
import com.hackathon.entity.enums.EvaluationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {
    List<Evaluation> findBySubmission_SubmissionIdAndStatus(int submissionSubmissionId, EvaluationStatus status);

    boolean existsBySubmission_TeamParticipant_CategoryRound_CategoryRoundIdAndStatus(Integer categoryRoundId, EvaluationStatus status);
}
