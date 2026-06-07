package com.hackathon.dto.criteria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
// TRA VE SAU KHI LUU
public class EvaluationCriteriaResponseDTO {
    private Integer evaluationId;
    private Integer criteriaSetId;
    private Integer eventId;
    private String evaluationName;
    private List<EvaluationItemResponseDTO> items;


    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter
    public static class EvaluationItemResponseDTO {
        private Integer evaluationItemId;
        private Integer criteriaDetailId;
        private String criteriaName;
        private String description;
        private BigDecimal maxScore;
    }
}
