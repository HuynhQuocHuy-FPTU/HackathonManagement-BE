package com.hackathon.dto.criteria;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriteriaResponseDTO {

    private int criteriaDetailId;

    private double customWeight;

    private String criteriaDetailName;

    private String description;

}
