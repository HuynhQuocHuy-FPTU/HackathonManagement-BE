package com.hackathon.dto.round;


import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import jakarta.validation.constraints.FutureOrPresent;
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
    private String roundName;

    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime startDate;
    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime endDate;

    private String advancementRule;

    private Integer topN;

    private Integer criteriaSetId;

    private Integer orderIndex;
    @FutureOrPresent(message = "Ngày phải là thời điểm trong tương lai")
    private LocalDateTime submissionDeadline;

    private List<EvaluationCriteriaRequestDTO> customCriteriaDetatils;

    private List<CategoryExpertAssignRequestDTO> categoryExperts;

}
