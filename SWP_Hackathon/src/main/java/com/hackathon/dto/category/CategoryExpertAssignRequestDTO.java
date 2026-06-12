package com.hackathon.dto.category;

import com.hackathon.dto.expert.ExpertAssginmentRequestDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryExpertAssignRequestDTO {
    @NotNull(message = "Category id is required")
    private Integer categoryId;

    @NotEmpty(message = "Experts is required")
    private List<ExpertAssginmentRequestDTO> experts;
}
