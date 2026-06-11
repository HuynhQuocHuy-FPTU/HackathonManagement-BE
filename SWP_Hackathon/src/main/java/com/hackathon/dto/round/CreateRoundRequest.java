package com.hackathon.dto.round;

import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateRoundRequest {
    @NotBlank(message = "Round name is required")
    private String roundName;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotBlank(message = "Advancement rule is required")
    private String advancementRule;

    @NotEmpty(message = "List Category is required")
    private List<String> appliedListCategoryNames;

    @NotNull(message = "Criteria_Set is required")
    private Integer criteriaSetId;


    @NotNull(message = "Order index is required")
    private Integer orderIndex;

    private List<EvaluationCriteriaRequestDTO> customCriteriaDetatils;

}
