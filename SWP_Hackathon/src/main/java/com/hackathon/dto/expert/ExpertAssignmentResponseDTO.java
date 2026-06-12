package com.hackathon.dto.expert;

import com.hackathon.entity.enums.ExpertRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertAssignmentResponseDTO {
    private Integer expertId;
    private String expertName;
    private ExpertRole role;
}
