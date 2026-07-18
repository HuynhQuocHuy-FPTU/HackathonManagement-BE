package com.hackathon.service;

import com.hackathon.dto.evaluation.EvaluationResponse;
import com.hackathon.security.CustomUserDetails;

import java.util.List;

public interface EvaluationDetailService {
    List<EvaluationResponse> getEvaluated(Integer submissionId);

}
