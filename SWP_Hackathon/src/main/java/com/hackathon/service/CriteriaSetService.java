package com.hackathon.service;

import com.hackathon.dto.criteria.response.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.response.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.response.CriteriaSetResponseDTO;
import java.util.List;

public interface CriteriaSetService {

    // Lay tat ca thong tin trong bo tieu chi goc (template).
    List<CriteriaSetResponseDTO> getAllCriteriaSets();

    //   Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    List<CriteriaSetDetailResponseDTO> getAllCriteriaSetDetail();

    // Lay tat ca thong tin trong tieu chi chi tiet(detail) hien thi
    List<CriteriaDetailResponseDTO> getAllCriteriaDetail();

    //Lay thong tin Criteria_Detail bang ID cua bo tieu chi (Set).
    List<CriteriaDetailResponseDTO>getCriteriaDetailById(Integer criteriaSetId);
}
