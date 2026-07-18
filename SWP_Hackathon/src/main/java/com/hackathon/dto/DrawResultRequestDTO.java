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
public class DrawResultRequestDTO {
    @NotNull(message = "Hạng mục là bắt buộc")
    private Integer categoryId;

    @NotEmpty(message = "Danh sách team không được để trống")
    private List<Integer> registrationId;
}

