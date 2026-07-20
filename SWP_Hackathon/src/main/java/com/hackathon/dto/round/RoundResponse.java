package com.hackathon.dto.round;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hackathon.dto.category.CategoryExpertAssignResponseDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaResponseDTO;
import com.hackathon.entity.Round;
import com.hackathon.entity.enums.FileType;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.entity.enums.SubmissionType;
import jakarta.validation.constraints.FutureOrPresent;
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

    private Integer topN;

    private Integer orderIndex;

    private RoundStatus status;

    private LocalDateTime submissionDeadline;

    private LocalDateTime evaluationDeadline;

    private LocalDateTime resolveAppealDeadline;

    private SubmissionType submissionType;

    private List<FileType> allowedFileTypes;

    private Integer maxFileCount;

    private String description;

    private List<EvaluationCriteriaResponseDTO> customCriteriaDetatils;
    private List<CategoryExpertAssignResponseDTO> categoryExperts;

    public RoundResponse(Round round, List<EvaluationCriteriaResponseDTO> criteriaList, List<CategoryExpertAssignResponseDTO> experts){
        this.roundId = round.getRoundId();
        this.roundName = round.getRoundName();
        this.startDate = round.getStartTime();
        this.endDate = round.getEndTime();
        this.eventID = round.getHackathonEvent().getEventId();
        this.orderIndex = round.getOrderIndex();
        this.topN = round.getTopN();
        this.status = round.getStatus();
        this.criteriaSetId = round.getCriteriaSet().getCriteriaSetId();
        this.submissionType = round.getSubmissionType();
        this.allowedFileTypes = round.getAllowedFileType();
        this.maxFileCount = round.getMaxFileCount();
        this.advancementRule = round.getAdvancementRule();
        this.submissionDeadline = round.getSubmissionDeadline();
        this.customCriteriaDetatils = criteriaList;
        this.categoryExperts = experts;
        this.evaluationDeadline = round.getEvaluationDeadline();
        this.resolveAppealDeadline = round.getResolveAppealDeadline();
        this.description = round.getDescription();
    }
}
