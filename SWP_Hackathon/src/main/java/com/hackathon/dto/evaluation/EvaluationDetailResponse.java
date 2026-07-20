package com.hackathon.dto.evaluation;

import com.hackathon.entity.EvaluationDetail;
import com.hackathon.entity.enums.CriteriaType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
//@NoArgsConstructor
@AllArgsConstructor
@Builder

public class EvaluationDetailResponse {
    private Integer evaluationDetailId;
    private String criteriaName;
    private BigDecimal weight;
    private CriteriaType criteriaType;
    private String criteriaDescription;
    private BigDecimal score;
    private String comment;
}
