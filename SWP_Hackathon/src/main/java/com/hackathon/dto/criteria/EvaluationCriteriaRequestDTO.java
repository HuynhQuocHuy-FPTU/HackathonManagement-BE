package com.hackathon.dto.criteria;
import com.hackathon.entity.enums.CriteriaType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class EvaluationCriteriaRequestDTO {
    @NotBlank(message = "Tên tiêu chí là bắt buộc")
    private String criteriaName;
    @NotNull(message = "Trọng số của tiêu chí là bắt buộc")
    // Trọng số không được nhỏ hơn 0.0 (0%)
    @DecimalMin(value = "0.0", message = "Trọng số (Weight) phải lớn hơn hoặc bằng 0")
    // Trọng số không được phép vượt quá 1.0 (100%)
    @DecimalMax(value = "100.0", message = "Trọng số (Weight) không được vượt quá 100")
    private BigDecimal customWeight;

    @NotBlank(message = "Loại tiêu chí là bắt buộc")
    private CriteriaType type;

    @NotBlank(message = "Mô tả là bắt buộc")
    private String description;

}
