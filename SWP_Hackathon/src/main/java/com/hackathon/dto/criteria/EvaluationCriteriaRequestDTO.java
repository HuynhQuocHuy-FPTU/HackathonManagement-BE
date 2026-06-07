package com.hackathon.dto.criteria;
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

}
