package com.hackathon.dto.round;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)

public class RoundResponse {

    private Integer roundId;

    private String roundName;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer eventID;

    private String advancementRule;

    private List<String> appliedListCategoryNames;

    private Integer criteriaSetId;

    private Integer orderIndex;

    private List<EvaluationCriteriaResponseDTO> customCriteriaDetatils;
}
