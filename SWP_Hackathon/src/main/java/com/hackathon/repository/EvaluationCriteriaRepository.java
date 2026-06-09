package com.hackathon.repository;

import com.hackathon.entity.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Integer> {
    List<EvaluationCriteria> findByEvaluationCriteriaId(Integer evaluationCriteriaId);
    List<EvaluationCriteria> findByRound_RoundId(Integer roundId);
}
