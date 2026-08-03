package com.hackathon.dto.evaluation;

import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.CriteriaType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class EvaluationAuditAttemptResponse {
    private Integer attemptId;
    private Integer evaluationId;
    private Integer eventId;
    private Integer roundId;
    private Integer attemptNumber;
    private CriteriaType criteriaType;
    private BigDecimal totalScore;
    private String totalComment;
    private EvaluationStatus status;
    private AuditAction action;
    private String actorName;
    private LocalDateTime createdAt;
}
