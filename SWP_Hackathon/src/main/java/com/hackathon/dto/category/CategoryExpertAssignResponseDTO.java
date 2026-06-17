package com.hackathon.dto.category;

import com.hackathon.dto.expert.ExpertAssignmentResponseDTO;
import lombok.*;

import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CategoryExpertAssignResponseDTO {

    private Integer categoryId;

    private List<ExpertAssignmentResponseDTO> experts;
}
