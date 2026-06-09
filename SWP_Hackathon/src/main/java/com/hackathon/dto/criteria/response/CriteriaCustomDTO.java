package com.hackathon.dto.criteria.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CriteriaCustomDTO {
     private Integer criteriaDetailId; // Tham chiếu tiêu chí gốc
        private String criteriaName;
        private String description;
        private BigDecimal maxScore;
        private BigDecimal weight;
        private Integer evaluationCriteriaId;
}
