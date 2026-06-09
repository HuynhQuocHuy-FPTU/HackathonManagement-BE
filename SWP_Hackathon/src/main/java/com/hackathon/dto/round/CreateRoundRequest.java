package com.hackathon.dto.round;

<<<<<<< HEAD
import com.hackathon.dto.criteria.request.CustomCriteriaRound;
=======
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
>>>>>>> d2901bebba4a026b6f8bf66193b8dfe738f6ae7e
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


    @NotNull(message = "Order index is required")
    private Integer orderIndex;

    private List<EvaluationCriteriaRequestDTO> customCriteriaDetatils;

}
