package com.hackathon.dto.round;


import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.entity.enums.FileType;
import com.hackathon.entity.enums.SubmissionType;
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

    private String description;

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

    private SubmissionType submissionType;

    private List<FileType> allowedFileTypes;

    private Integer maxFileCount;

    private Integer maxTotalSizeMb;

    private List<EvaluationCriteriaRequestDTO> customCriteriaDetatils;

    private List<CategoryExpertAssignRequestDTO> categoryExperts;

}
