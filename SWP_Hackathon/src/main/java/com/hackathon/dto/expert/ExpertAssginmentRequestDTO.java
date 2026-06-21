package com.hackathon.dto.expert;

import com.hackathon.entity.enums.ExpertRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class ExpertAssginmentRequestDTO {
    private Integer expertId;
    private ExpertRole role;
}
