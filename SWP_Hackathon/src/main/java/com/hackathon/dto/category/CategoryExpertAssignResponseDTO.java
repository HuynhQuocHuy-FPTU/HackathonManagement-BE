package com.hackathon.dto.category;

import com.hackathon.dto.expert.ExpertAssignmentResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryExpertAssignResponseDTO {

    private Integer categoryId;

    private List<ExpertAssignmentResponseDTO> experts;
}
