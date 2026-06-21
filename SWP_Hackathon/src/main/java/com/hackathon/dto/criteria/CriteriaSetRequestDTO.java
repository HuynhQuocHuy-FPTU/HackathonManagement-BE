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
public class CriteriaSetRequestDTO {
    private Integer criteriaSetId;

    @NotBlank(message = "Criteria Set Name is required")
    private String criteriaSetName;

    @NotNull(message = "MaxScore is required")
    @Min(value = 0, message = "MaxScore must be greater than or equal to 0")
    private Integer maxScore;
    private List<CriteriaDetailRequestDTO> criteriaDetails;

}
