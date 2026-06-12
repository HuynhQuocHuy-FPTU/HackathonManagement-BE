package com.hackathon.dto.criteria;

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
    @NotNull(message = "Criteria Name is required")
    private String criteriaSetName;
    @NotNull(message = "Max Score is required")
    private Integer maxScore;
    private List<CriteriaDetailRequestDTO> criteriaDetails;

}
