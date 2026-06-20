package com.hackathon.dto.criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class CriteriaSetResponseDTO {
    private Integer criteriaSetId;
    private String criteriaSetName;
    private int maxScore;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<CriteriaDetailResponseDTO> criteriaDetails;

}