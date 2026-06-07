package com.hackathon.controller;

import com.hackathon.dto.criteria.EvaluationCriteriaReponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.expection.ApiResponse;
import com.hackathon.service.EvaluationCriteriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/evaluation-criteria")
public class EvaluationCriteriaController {
    @Autowired
    private EvaluationCriteriaService evaluationCriteriaService;

    @PostMapping
    public ResponseEntity<ApiResponse<EvaluationCriteriaReponseDTO>> saveEvaluationCriteria(@RequestBody EvaluationCriteriaRequestDTO list) {
        EvaluationCriteriaReponseDTO response = evaluationCriteriaService.saveEvaluationCriteria(list);
        return ResponseEntity.ok(ApiResponse.success(response, "Save successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> updateEvaluationCriteria(@PathVariable Integer id, @RequestBody EvaluationCriteriaRequestDTO request) {
        evaluationCriteriaService.updateEvaluationCriteria(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Update sucessfully"));

    }

    // Xoa 1 tieu chi
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvaluationCriteria(
            @PathVariable Integer id) {
        evaluationCriteriaService.deleteEvaluationCriteriaById(id);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Delete successfully")
        );
    }

    //Xoa all tieu chi cua 1 round
    @DeleteMapping("/round/{roundId}")
    public ResponseEntity<ApiResponse<Void>> deleteAllCriteriaByRound(@PathVariable Integer roundId) {
        evaluationCriteriaService.deleteAllCriteriaByRoundId(roundId);
        return ResponseEntity.ok(
                ApiResponse.success(null, "Delete all criteria of round successfully")
        );
    }

}

