package com.hackathon.repository;

import com.hackathon.entity.Evaluation;
import com.hackathon.entity.EvaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationDetailRepository extends JpaRepository<EvaluationDetail, Integer> {
}
