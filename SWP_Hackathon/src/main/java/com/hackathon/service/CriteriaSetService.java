package com.hackathon.service;

import com.hackathon.dto.criteria.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import java.util.List;

public interface CriteriaSetService {
    CriteriaSetResponseDTO getCriteriaSetById(Integer criteriaSetId);
    List<CriteriaSetResponseDTO> getAllCriteriaSets();
    List<CriteriaSetDetailResponseDTO> getCriteriaDetailById(Integer criteriaSetId);
}
