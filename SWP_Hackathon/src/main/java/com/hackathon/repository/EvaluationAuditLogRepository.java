package com.hackathon.repository;

import com.hackathon.entity.EvaluationAuditLog;
import com.hackathon.entity.enums.CriteriaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationAuditLogRepository
        extends JpaRepository<EvaluationAuditLog, Integer> {

    @Query("SELECT COALESCE(MAX(log.attemptNumber), 0) "
            + "FROM EvaluationAuditLog log "
            + "WHERE log.evaluation.evaluationId = :evaluationId "
            + "AND log.criteriaType = :criteriaType")
    Integer findMaxAttemptNumber(
            @Param("evaluationId") Integer evaluationId,
            @Param("criteriaType") CriteriaType criteriaType);

    List<EvaluationAuditLog> findByEvaluation_EvaluationIdOrderByAttemptNumberDesc(
            Integer evaluationId);

    long countByEvaluation_EvaluationId(Integer evaluationId);
}
