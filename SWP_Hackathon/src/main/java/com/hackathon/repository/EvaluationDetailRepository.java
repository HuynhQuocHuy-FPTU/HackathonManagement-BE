package com.hackathon.repository;

import com.hackathon.entity.EvaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationDetailRepository  extends JpaRepository<EvaluationDetail, Integer> {
    List<EvaluationDetail> findByEvaluation_EvaluationId(int evaluationEvaluationId);
}
