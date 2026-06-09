package com.hackathon.dto.category.request;

import jakarta.validation.constraints.NotBlank;
<<<<<<< HEAD:SWP_Hackathon/src/main/java/com/hackathon/dto/category/request/CreateCategoryRequest.java
=======
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
>>>>>>> d2901bebba4a026b6f8bf66193b8dfe738f6ae7e:SWP_Hackathon/src/main/java/com/hackathon/dto/category/CreateCategoryRequest.java
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String categoryName;
    private Integer eventId;
}
