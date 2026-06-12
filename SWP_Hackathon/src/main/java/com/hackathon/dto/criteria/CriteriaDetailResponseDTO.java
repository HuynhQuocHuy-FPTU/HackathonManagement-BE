package com.hackathon.dto.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

// Dua du lieu CriteriaDetail tu Criteria Mau len UI
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CriteriaDetailResponseDTO {
    private Integer criteriaId;
    private String criteriaName;
    private BigDecimal weight;
    private String description;

}
