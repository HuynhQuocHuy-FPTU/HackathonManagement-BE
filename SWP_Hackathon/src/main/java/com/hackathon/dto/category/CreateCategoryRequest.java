package com.hackathon.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CreateCategoryRequest {
    private String categoryName;
}
