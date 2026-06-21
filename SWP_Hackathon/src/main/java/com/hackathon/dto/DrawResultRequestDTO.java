package com.hackathon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class DrawResultRequestDTO {
    @NotNull(message = "Registration id is required")
    private Integer registrationId;

    @NotNull(message = "Category id is required")
    private Integer categoryId;
}
