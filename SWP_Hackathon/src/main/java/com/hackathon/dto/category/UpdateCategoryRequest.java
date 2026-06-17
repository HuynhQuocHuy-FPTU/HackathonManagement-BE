package com.hackathon.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UpdateCategoryRequest {

    @NotNull(message = "Category id is required")
    private Integer categoryId;

    @NotBlank(message = "Category name is required")
    private String categoryName;
}
