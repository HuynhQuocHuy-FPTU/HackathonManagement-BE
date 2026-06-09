package com.hackathon.dto.round;

import com.hackathon.dto.criteria.request.CustomCriteriaRound;
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

    @NotNull(message = "Event ID is required")
    private Integer eventID;
    @NotBlank(message = "Advancement rule is required")
    private String advancementRule;

    @NotEmpty(message = "List Category is required")
    private List<String> appliedListCategoryNames;

    @NotNull(message = "Criteria_Set is required")
    private Integer criteriaSetId;


    private List<CustomCriteriaRound> customCriteriaRounds;

}
