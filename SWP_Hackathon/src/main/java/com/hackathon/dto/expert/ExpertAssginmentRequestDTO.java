package com.hackathon.dto.expert;

import com.hackathon.entity.enums.ExpertRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpertAssginmentRequestDTO {
    @NotNull(message = "Expert id is required")
    private Integer expertId;
    @NotEmpty(message = "Expert role is required")
    private ExpertRole role;
}
