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
public class EvaluationDetailDTO {
    private Integer evaluationDetailId;
    private String comment;
    private BigDecimal score;
    private Integer criteriaId;
    private Integer evaluationId;


}
