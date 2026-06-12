package com.hackathon.controller;


import com.hackathon.dto.criteria.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.exception.ApiResponse;
import com.hackathon.service.CriteriaSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/criteriaSet")

public class CriteriaSetController {
    @Autowired
    private CriteriaSetService criteriaSetService;

    //1. get all bo tieu chi hien co(criteria-set)
    @GetMapping
    public ResponseEntity<ApiResponse<List<CriteriaSetResponseDTO>>>getAllCriteriaSets() {
        List<CriteriaSetResponseDTO> list =  criteriaSetService.getAllCriteriaSets();
        return ResponseEntity.ok(ApiResponse.success(list, "Get All criteria-set successfully"));
    }
   //2. Lay all thong tin trong bo tiey chi chi tiet (criteria-detail)
    @GetMapping("criteria-detail")
    public ResponseEntity<ApiResponse<List<CriteriaDetailResponseDTO>>> getCriteriaSet() {
        List<CriteriaDetailResponseDTO> list = criteriaSetService.getAllCriteriaDetail();
        return ResponseEntity.ok(ApiResponse.success(list,"Get criteria-detail successfully"));
    }

    //3.   Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    @GetMapping("/with-details")
    public ResponseEntity<ApiResponse<List<CriteriaSetDetailResponseDTO> >>getAllCriteriaSetDetail(){
     List<CriteriaSetDetailResponseDTO>   list = criteriaSetService.getAllCriteriaSetDetail();
     return ResponseEntity.ok(ApiResponse.success(list,"Get All info about criteria-set and criteria-detail successfully"));

    }
    //4. Lay thong tin Criteria_Detail bang ID cua bo tieu chi (Set).
    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse< List<CriteriaDetailResponseDTO>>> getCriteriaDetailByCriteriaSet(@PathVariable ("id") Integer criteriaSetId ){
        List<CriteriaDetailResponseDTO> list = criteriaSetService.getCriteriaDetailById(criteriaSetId);
        return ResponseEntity.ok(ApiResponse.success(list,"Get all info of Criteria-Detail by CriteriaSet"));
    }
}
