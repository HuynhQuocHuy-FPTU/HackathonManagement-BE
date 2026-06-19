package com.hackathon.dto.criteria;

import com.hackathon.entity.enums.CriteriaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

// Dua du lieu CriteriaDetail tu Criteria Mau len UI
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CriteriaDetailResponseDTO {
    private Integer criteriaId;

    @NotBlank(message = "Criteria Detail Name is required")
    private String criteriaName;

    @NotBlank(message = "Weight is required")
    @Min(value = 0, message = "Weight must be greater than or equal to 0")
    private BigDecimal weight;

    private CriteriaType type;

    private String description;

}
