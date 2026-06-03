package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class EvaluationDTO {
    private Integer evaluationId;
    private String comment;
    private BigDecimal totalScore;
    private Integer expertId;
    private Integer submissionId;


}
