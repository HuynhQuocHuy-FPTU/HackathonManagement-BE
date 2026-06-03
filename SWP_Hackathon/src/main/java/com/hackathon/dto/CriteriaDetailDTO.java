package com.hackathon.dto;

import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CriteriaDetailDTO {
    private Integer criteriaId;
    private String criteriaName;
    private BigDecimal weight;

}
