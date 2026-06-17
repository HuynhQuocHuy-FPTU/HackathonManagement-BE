package com.hackathon.dto.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CategoryResponse {
    private Integer categoryId;
    private String categoryName;
}
