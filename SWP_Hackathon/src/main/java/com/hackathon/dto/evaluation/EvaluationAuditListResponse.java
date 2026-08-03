package com.hackathon.dto.evaluation;

import com.hackathon.entity.enums.EvaluationStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EvaluationAuditListResponse {
    private Integer evaluationId;
    private Integer submissionId;
    private Integer teamId;
    private String teamName;
    private Integer judgeId;
    private String judgeName;
    private Integer categoryRoundId;
    private String categoryName;
    private BigDecimal currentScore;
    private EvaluationStatus currentStatus;
    private long totalAttempts;
}
