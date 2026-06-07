package com.hackathon.dto.criteria;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomCriteriaRound {
    @NotNull(message = "Criteria detail id is required")
    private int criteriaDetailId;

    @NotNull(message = "Custom weight is required")
    private double customWeight;
}
