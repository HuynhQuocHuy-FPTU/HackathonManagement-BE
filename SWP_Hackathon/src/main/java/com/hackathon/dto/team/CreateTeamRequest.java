package com.hackathon.dto.team;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CreateTeamRequest {
    private Integer eventId;

    @NotBlank(message = "Team name is required")
    private String teamName;

    @NotNull(message = "Member list is required")
    @Size(min = 1, message = "Team must have at least 1 member besides leader")
    private List<@NotBlank @Email String> memberEmails;

}
