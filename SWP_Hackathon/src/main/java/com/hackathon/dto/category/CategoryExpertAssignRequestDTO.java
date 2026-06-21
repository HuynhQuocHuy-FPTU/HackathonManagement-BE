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
    private Integer categoryId;

    private List<ExpertAssginmentRequestDTO> experts;
}
