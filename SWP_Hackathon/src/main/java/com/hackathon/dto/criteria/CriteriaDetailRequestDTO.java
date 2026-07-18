package com.hackathon.dto.criteria;

import com.hackathon.entity.enums.CriteriaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
public class CriteriaDetailRequestDTO {
    private Integer criteriaId;

    @NotBlank(message = "Tên bộ tiêu chí là bắt buộc")
    private String criteriaName;

    @NotBlank(message = "Trọng số (Weight) là bắt buộc")
    @Min(value = 0, message = "Trọng số (Weight) bắt buộc lớn hơn hoặc bằng 0")
    private BigDecimal weight;

    @NotBlank(message = "Loại tiêu chí (Criteria type) là bắt buộc")
    private CriteriaType type;

    @NotBlank(message = "Mô tả là bắt buộc")
    private String description;
}
