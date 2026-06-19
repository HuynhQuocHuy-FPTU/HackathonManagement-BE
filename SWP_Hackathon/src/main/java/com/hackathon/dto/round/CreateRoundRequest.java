package com.hackathon.dto.round;


import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CreateRoundRequest {
    @NotBlank(message = "Round name is required")
    private String roundName;

    @NotNull(message = "Start date is required")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    private LocalDateTime endDate;

    @NotBlank(message = "Advancement rule is required")
    private String advancementRule;

    @NotNull(message = "Top N is required")
    private Integer topN;

    @NotNull(message = "Criteria_Set is required")
    private Integer criteriaSetId;

    @NotNull(message = "Order index is required")
    private Integer orderIndex;

    @NotNull(message = "Submission deadline is required")
    private LocalDateTime submissionDeadline;

    private List<EvaluationCriteriaRequestDTO> customCriteriaDetatils;

    private List<CategoryExpertAssignRequestDTO> categoryExperts;

}
