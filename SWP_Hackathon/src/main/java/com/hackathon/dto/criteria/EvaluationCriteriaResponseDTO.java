package com.hackathon.dto.criteria;


import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class EvaluationCriteriaResponseDTO {

    private Integer evaluationCriteriaId;

    private BigDecimal customWeight;

    private String criteriaDetailName;

    private String description;

}
