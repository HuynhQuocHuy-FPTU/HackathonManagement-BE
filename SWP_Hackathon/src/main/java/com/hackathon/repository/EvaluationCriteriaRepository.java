package com.hackathon.repository;

import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.entity.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
@Repository
public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Integer> {
    @Modifying
    @Transactional
    @Query("DELETE FROM EvaluationCriteria  ec WHERE ec.round.hackathonEvent.eventId = :eventId")
    void deleteByEventId(@Param(("eventId") )Integer eventId);

    void deleteByRound_RoundId(Integer roundRoundId);

    List<EvaluationCriteria> findByRound_RoundId(Integer roundRoundId);
}
