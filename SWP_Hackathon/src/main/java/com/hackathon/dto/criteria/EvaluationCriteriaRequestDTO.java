package com.hackathon.dto.criteria;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotNull(message = "Criteria detail id is required")
    private int criteriaDetailId;

    @NotNull(message = "Custom weight is required")
    @Min(value = 0, message = "Weight must be greater than or equal to 0")
    private double customWeight;

    @NotBlank(message = "Description is required")
    private String description;

}
