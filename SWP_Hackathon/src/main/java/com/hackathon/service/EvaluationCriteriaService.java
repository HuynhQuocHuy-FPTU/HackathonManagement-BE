package com.hackathon.service;

import com.hackathon.dto.criteria.EvaluationCriteriaReponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;

import java.util.List;

public interface EvaluationCriteriaService {
    List<EvaluationCriteriaRequestDTO> getEvaluationCriteriaByCriteriaId(Integer criteriaId);

    EvaluationCriteriaReponseDTO saveEvaluationCriteria(EvaluationCriteriaRequestDTO evaluationCriteriaRequestDTO);

    // Lấy theo eventId — biết event này đang dùng bộ tiêu chí nào
    EvaluationCriteriaReponseDTO getByEventId(Integer eventId);

    //Update
    void updateEvaluationCriteria(EvaluationCriteriaRequestDTO evaluationCriteriaRequestDTO);

    //Delete
    void deleteEvaluationCriteriaById(Integer id);

    //Delete 1 item
    void deleteAllCriteriaByRoundId(Integer id);
}
