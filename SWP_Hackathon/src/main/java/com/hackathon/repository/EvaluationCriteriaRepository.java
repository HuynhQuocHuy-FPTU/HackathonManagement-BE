package com.hackathon.repository;

import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.entity.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Integer> {
    void deleteEvaluationCriteriaByRound_HackathonEvent_EventId(int roundHackathonEventEventId);
}
