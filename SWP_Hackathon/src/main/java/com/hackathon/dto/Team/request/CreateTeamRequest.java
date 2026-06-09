package com.hackathon.dto.Team.request;

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
