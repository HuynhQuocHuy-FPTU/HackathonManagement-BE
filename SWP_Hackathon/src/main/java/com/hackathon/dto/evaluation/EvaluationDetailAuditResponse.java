package com.hackathon.dto.evaluation;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EvaluationDetailAuditResponse {
    private Long detailAuditId;
    private Integer attemptId;
    private Integer evaluationDetailId;
    private Integer criteriaId;
    private String criteriaName;
    private BigDecimal score;
    private String comment;
    private BigDecimal criteriaWeight;
}
