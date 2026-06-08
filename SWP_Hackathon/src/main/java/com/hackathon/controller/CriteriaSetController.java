package com.hackathon.controller;


import com.hackathon.dto.criteria.CriteriaDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetDetailResponseDTO;
import com.hackathon.dto.criteria.CriteriaSetResponseDTO;
import com.hackathon.service.CriteriaSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/criteriaSet")

public class CriteriaSetController {
    @Autowired
    private CriteriaSetService criteriaSetService;

    //1. get all bo tieu chi hien co(criteria-set)
    @GetMapping
    public List<CriteriaSetResponseDTO> getAllCriteriaSets() {

        return criteriaSetService.getAllCriteriaSets();
    }
   //2. get  bo tiey chi chi tiet (criteria-detail)
    @GetMapping("criteria-detail")
    public List<CriteriaDetailResponseDTO>  getCriteriaSet() {
        return criteriaSetService.getAllCriteriaDetail();
    }

    //3.   Lay tat ca thong tin trong bo tieu chi goc(template) va tieu chi chi tiet trong template
    @GetMapping("/with-details")
    public List<CriteriaSetDetailResponseDTO> getAllCriteriaSetDetail(){
        return criteriaSetService.getAllCriteriaSetDetail();
    }
    //4. Lay thong tin Criteria_Detail bang ID cua bo tieu chi (Set).
    @GetMapping("/{id}/detail")
    public List<CriteriaDetailResponseDTO> getCriteriaDetailByCriteriaSet(@PathVariable ("id") Integer criteriaSetId ){
        return criteriaSetService.getCriteriaDetailById(criteriaSetId);

    }
}
