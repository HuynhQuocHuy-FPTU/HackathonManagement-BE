package com.hackathon.dto;

import lombok.*;

import java.math.BigDecimal;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter

public class CriteriaSetDTO {
    private Integer criteriaSetId;
    private BigDecimal maxScore;
    private Integer coordinatorId;


}
