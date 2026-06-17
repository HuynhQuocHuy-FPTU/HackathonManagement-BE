package com.hackathon.dto.criteria;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class EvaluationCriteriaRequestDTO {
    @NotNull(message = "Criteria detail id is required")
    private Integer criteriaDetailId;

    @NotNull(message = "Custom weight is required")
    // Trọng số không được nhỏ hơn 0.0 (0%)
    @DecimalMin(value = "0.0", message = "Trọng số phải lớn hơn hoặc bằng 0")
    // Trọng số không được phép vượt quá 1.0 (100%)
    @DecimalMax(value = "1.0", message = "Trọng số không được vượt quá 1.0 (100%)")
    private Double customWeight;

    @NotBlank(message = "Description is required")
    private String description;

}
