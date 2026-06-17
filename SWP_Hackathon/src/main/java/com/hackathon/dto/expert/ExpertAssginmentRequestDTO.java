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
    @NotNull(message = "Expert id is required")
    private Integer expertId;
    @NotEmpty(message = "Expert role is required")
    private ExpertRole role;
}
