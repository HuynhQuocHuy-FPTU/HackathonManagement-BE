package com.hackathon.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CreateCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String categoryName;
//    private Integer eventId;
}
