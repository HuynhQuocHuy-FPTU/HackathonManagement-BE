package com.hackathon.repository;

import com.hackathon.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Integer> {

    @Query("SELECT e FROM Evaluation e " +
            "WHERE e.expertAssign.assignId = :assignId " +
            "AND e.submission.submissionId = :submissionId")
    Optional<Evaluation> findByExpertAssignIdAndSubmissionId(
            @Param("assignId") Integer assignId,
            @Param("submissionId") Integer submissionId);
}
