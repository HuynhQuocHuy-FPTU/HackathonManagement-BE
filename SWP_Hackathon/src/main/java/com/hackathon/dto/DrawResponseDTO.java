package com.hackathon.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class DrawResponseDTO {
    private Integer categoryId;
    private String categoryName;
    private List<Integer> registrationId;
}

