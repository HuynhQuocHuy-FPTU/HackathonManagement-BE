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
public class CriteriaSetResponseDTO {
    private Integer criteriaSetId;
    private String criteriaName;
    private Integer maxScore;

}