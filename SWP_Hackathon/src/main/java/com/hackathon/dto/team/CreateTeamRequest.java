package com.hackathon.dto.team;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTeamRequest {
    private Integer eventID;
    @NotBlank(message = "Team name is required")
    private String teamName;


}
