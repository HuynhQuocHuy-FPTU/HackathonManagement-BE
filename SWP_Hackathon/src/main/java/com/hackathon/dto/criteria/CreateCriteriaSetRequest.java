package com.hackathon.dto.criteria;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter

public class CreateCriteriaSetRequest {
    @NotBlank(message = "Tên bộ tiêu chí yêu cầu là bắt buộc")
    private String criteriaSetName;

    @NotNull(message = "Điểm tối đa (MaxScore) là bắt buộc")
    @Min(value = 0, message = "Điểm tối đa (MaxScore) bắt buộc lớn hơn hoặc bằng 0")
    private Integer maxScore;
    private List<CriteriaDetailRequestDTO> criteriaDetails;
}
