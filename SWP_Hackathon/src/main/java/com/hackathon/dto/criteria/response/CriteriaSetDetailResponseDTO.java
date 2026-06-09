package com.hackathon.dto.criteria.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
// Dua du lieu CriteriaSet + CriteriaDetail tu Criteria Mau len UI
public class CriteriaSetDetailResponseDTO {
    private Integer criteriaSetId;
    private String criteriaSetName;
    private BigDecimal maxScore;
    private List<CriteriaDetailResponseDTO> criteriaDetails;
}
