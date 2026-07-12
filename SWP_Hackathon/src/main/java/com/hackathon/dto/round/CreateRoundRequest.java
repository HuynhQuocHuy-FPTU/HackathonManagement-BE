package com.hackathon.dto.round;


import com.hackathon.dto.category.CategoryExpertAssignRequestDTO;
import com.hackathon.dto.criteria.EvaluationCriteriaRequestDTO;
import com.hackathon.entity.enums.FileType;
import com.hackathon.entity.enums.SubmissionType;
import jakarta.validation.constraints.*;
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

    @Min(value = 0, message = "Giá trị của số lượng file phải lớn hơn 0")
    private Integer maxFileCount;
    @Min(value = 0, message = "Giá trị tổng size của file phải lớn hơn 0 MB")
    @Max(value = 1024, message = "Giá trị tổng size của file phải nhỏ hơn 1024 MB")
    private Integer maxTotalSizeMb;

    private List<EvaluationCriteriaRequestDTO> customCriteriaDetatils;

    private List<CategoryExpertAssignRequestDTO> categoryExperts;

}
