package com.hackathon.dto.criteria;


import com.hackathon.entity.enums.CriteriaType;
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

    private CriteriaType type;

    private String description;

}
