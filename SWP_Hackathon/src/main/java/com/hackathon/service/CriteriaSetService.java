package com.hackathon.service;

import com.hackathon.dto.criteria.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetRequestDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.entity.CriteriaSet;

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

    // Tao bo tieu chi danh gia (Template)
    CriteriaSetResponseDTO createCriteriaSet(CriteriaSetRequestDTO request);

    //Update bo tieu chi
    CriteriaSetResponseDTO updateCriteriaSet(CriteriaSetRequestDTO request);

    // Xoa bo tieu chi
    void deleteCriteriaSet(Integer criteriaSetId);

    //Them bo tieu chi
//    void addCriteriaSet (CriteriaSetRequestDTO request);
}
