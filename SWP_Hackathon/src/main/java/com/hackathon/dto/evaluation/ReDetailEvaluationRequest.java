package com.hackathon.dto.evaluation;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ReDetailEvaluationRequest {
    private Integer requestId;
    private BigDecimal score;
    private String comment;
    private List<EvaluationCriteriaRequest> criteriaScores;

    @Getter
    @Setter
    public static class EvaluationCriteriaRequest {
        private Integer evaluationDetailId;
        private BigDecimal newScore;
    }
}
