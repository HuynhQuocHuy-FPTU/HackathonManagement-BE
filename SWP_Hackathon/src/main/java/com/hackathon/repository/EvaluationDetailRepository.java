package com.hackathon.repository;

import com.hackathon.entity.EvaluationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EvaluationDetailRepository  extends JpaRepository<EvaluationDetail, Integer> {
}
