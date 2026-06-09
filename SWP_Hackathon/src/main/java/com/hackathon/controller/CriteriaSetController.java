package com.hackathon.controller;


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

    // hien thi all bo tieu chi hien co
    @GetMapping
    public List<CriteriaSetResponseDTO> getAllCriteriaSets() {
        return criteriaSetService.getAllCriteriaSets();
    }

    // lay bo tiey chi theo id
    @GetMapping("/{id}")
    public CriteriaSetResponseDTO getCriteriaSet(@PathVariable("id") Integer criteriaSetId) {
        return criteriaSetService.getCriteriaSetById(criteriaSetId);
    }

    // lay bo tieu chi chi tiet , khi click vao bo tieu chi thi se hien thi chi tiet bo tieu chi do
    @GetMapping("/{id}/details")
    public List<CriteriaSetDetailResponseDTO> getCriteriaSetDetails(@PathVariable("id") Integer criteriaSetId) {
        return criteriaSetService.getCriteriaDetailById(criteriaSetId);
    }

}
