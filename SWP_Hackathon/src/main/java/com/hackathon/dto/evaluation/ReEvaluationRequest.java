package com.hackathon.dto.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ReEvaluationRequest {
    private String comment;
    private Integer requestId;
    @NotEmpty(message = "Danh sách điểm số tiêu chí không được để trống")
    @Valid
    private List<CriteriaScoreRequest> criteriaScores;

}
