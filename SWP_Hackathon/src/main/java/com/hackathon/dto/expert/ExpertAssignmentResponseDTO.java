package com.hackathon.dto.expert;

import com.hackathon.entity.enums.ExpertRole;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class ExpertAssignmentResponseDTO {
    private Integer expertId;
    private String expertName;
    private ExpertRole role;
}
