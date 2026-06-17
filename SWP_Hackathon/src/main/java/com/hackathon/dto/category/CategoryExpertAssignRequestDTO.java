package com.hackathon.dto.category;

import com.hackathon.dto.expert.ExpertAssginmentRequestDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CategoryExpertAssignRequestDTO {
    @NotNull(message = "Category id is required")
    private Integer categoryId;

    @NotEmpty(message = "Experts is required")
    private List<ExpertAssginmentRequestDTO> experts;
}
