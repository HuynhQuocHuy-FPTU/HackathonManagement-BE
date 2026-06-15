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
public class TeamRequest {
//    private String teamName;
    @NotBlank(message = "StudentCode is required")
    private String studentCode;
}
