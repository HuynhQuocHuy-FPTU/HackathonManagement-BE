package com.hackathon.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamJoinRequest(
        @NotBlank(message = "Lý do tham gia team không được để trống")
        @Size(
                min = 10,
                max = 500,
                message = "Lý do tham gia phải từ 10 đến 500 ký tự"
        )
        String reason
) {
}
