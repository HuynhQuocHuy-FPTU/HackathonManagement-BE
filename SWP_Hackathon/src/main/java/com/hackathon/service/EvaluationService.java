package com.hackathon.service;

import com.hackathon.security.CustomUserDetails;

public interface EvaluationService {
    void checkJudgesCompletedEvaluation(CustomUserDetails userDetails, Integer roundId);
}
