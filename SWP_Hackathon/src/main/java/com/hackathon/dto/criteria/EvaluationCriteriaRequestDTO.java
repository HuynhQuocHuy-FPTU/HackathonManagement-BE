package com.hackathon.dto.criteria;

import com.hackathon.entity.Round;
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
public class EvaluationCriteriaRequestDTO {
    private Integer criteriaSetId;
    private Integer evaluationCriteriaId;
    private  Integer eventId;
    private Integer roundId;
    private String criteriaName;
    private BigDecimal weight;
    private String description;
    private List<CriteriaCustomDTO> criteriaList;  // lay ds da duoc chinh sua

    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    @Getter

    public static class CriteriaCustomDTO {
        private Integer criteriaDetailId; // Tham chiếu tiêu chí gốc
        private String criteriaName;
        private String description;
        private BigDecimal maxScore;
        private BigDecimal weight;
        private Integer evaluationCriteriaId;

    }
}
