package com.hackathon.repository;

import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.entity.EvaluationCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
@Repository
public interface EvaluationCriteriaRepository extends JpaRepository<EvaluationCriteria, Integer> {
    void deleteEvaluationCriteriaByRound_HackathonEvent_EventId(int roundHackathonEventEventId);
}
