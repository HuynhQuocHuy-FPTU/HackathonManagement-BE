package com.hackathon.service;

import com.hackathon.dto.criteria.CreateCriteriaSetRequest;
import com.hackathon.dto.criteria.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetRequestDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.dto.history.CriteriaHistoryResponse;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface CriteriaSetService {

    // Lay tat ca thong tin trong bo tieu chi goc (template).
    List<CriteriaSetResponseDTO> getAllCriteriaSets();

    //   Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    List<CriteriaSetResponseDTO> getAllCriteriaSetDetail();

    // Lay tat ca thong tin trong tieu chi chi tiet(detail) hien thi
    List<CriteriaDetailResponseDTO> getAllCriteriaDetail();

    //Lay thong tin Criteria_Detail bang ID cua bo tieu chi (Set).
    CriteriaSetResponseDTO getCriteriaDetailById(Integer criteriaSetId);

    // Tao bo tieu chi danh gia (Template)
    CriteriaSetResponseDTO createCriteriaSet(CreateCriteriaSetRequest request, CustomUserDetails userDetailist);

    //Update bo tieu chi
    CriteriaSetResponseDTO updateCriteriaSet(CriteriaSetRequestDTO request,CustomUserDetails userDetailist );

    // Xoa bo tieu chi
    void deleteCriteriaSet(Integer criteriaSetId,CustomUserDetails userDetails);

    // Lich su bo tieu chi
    CriteriaHistoryResponse getHistoryCriteria(CustomUserDetails userDetails,  Integer criteriaSetId);

}
