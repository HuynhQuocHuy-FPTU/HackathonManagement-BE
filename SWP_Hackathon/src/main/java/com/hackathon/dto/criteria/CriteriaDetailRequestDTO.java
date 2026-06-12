package com.hackathon.dto.criteria;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class CriteriaDetailRequestDTO {
    private String criteriaName;
    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be greater than or equal to 0")
    private BigDecimal weight;
    @NotBlank(message = "Description is required")
    private String description;
}
