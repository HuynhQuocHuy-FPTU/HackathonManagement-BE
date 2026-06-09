package com.hackathon.service;

import com.hackathon.dto.criteria.response.EvaluationCriteriaResponseDTO;
import com.hackathon.dto.criteria.request.EvaluationCriteriaRequestDTO;

import java.util.List;

public interface EvaluationCriteriaService {
    List<EvaluationCriteriaRequestDTO> getEvaluationCriteriaByCriteriaId(Integer criteriaId);

    EvaluationCriteriaResponseDTO saveEvaluationCriteria(EvaluationCriteriaRequestDTO evaluationCriteriaRequestDTO);

    // Lấy theo eventId — biết event này đang dùng bộ tiêu chí nào
    EvaluationCriteriaResponseDTO getByEventId(Integer eventId);

    //Update
    void updateEvaluationCriteria(EvaluationCriteriaRequestDTO evaluationCriteriaRequestDTO);

    //Delete
    void deleteEvaluationCriteriaById(Integer id);

    //Delete 1 item
    void deleteAllCriteriaByRoundId(Integer id);
}
