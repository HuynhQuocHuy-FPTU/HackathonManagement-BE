package com.hackathon.dto.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CriteriaSetRequestDTO {
    private Integer criteriaSetId;
    private BigDecimal maxScore;
    private Integer coordinatorId;

}
