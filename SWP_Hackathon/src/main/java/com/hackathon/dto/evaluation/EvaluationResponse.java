package com.hackathon.dto.evaluation;

import com.hackathon.dto.submission.SubmissionResponse;
import com.hackathon.entity.enums.EvaluationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
public class EvaluationResponse {
    private Integer evaluationId;
    private BigDecimal totalScore;
    private EvaluationStatus status;
    private String comment;
    private SubmissionResponse submissions;
    private List<EvaluationDetailResponse> listEvaluationDetail;

}
