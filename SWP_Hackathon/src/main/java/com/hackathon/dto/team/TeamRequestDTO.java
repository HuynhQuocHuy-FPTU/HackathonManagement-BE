package com.hackathon.dto.team;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeamRequestDTO {
    @NotBlank(message = "Mã số sing viên (StudentCode) là bắt buộc")
    private String studentCode;
}
