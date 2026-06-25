package com.hackathon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class DrawResultRequestDTO {


    @NotNull(message = "Category id is required")
    private Integer categoryId;

    @NotNull(message = "Registration id is required")
    private List<Integer> registrationId;
}

