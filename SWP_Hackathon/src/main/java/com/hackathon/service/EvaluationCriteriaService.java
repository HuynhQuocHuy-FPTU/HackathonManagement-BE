package com.hackathon.service;

import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.entity.EvaluationCriteria;
import com.hackathon.entity.Round;

import java.util.List;

public interface EvaluationCriteriaService {
        public EvaluationCriteria createEvaluationCritera(EvaluationCriteriaRequestDTO request, int criteriaSetId, Round round);
        public EvaluationCriteriaResponseDTO mapToResponse(EvaluationCriteria evaluationCriteria);

        List<EvaluationCriteriaResponseDTO> getEvaluationCriteriaResponse(Round round);

        public void deletedEvaluationCriteria(Integer roundId);
}
