package com.hackathon.dto.team;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAdvancementResultDTO {
    private Integer categoryRoundID;
    private List<AdvancedTeamDTO> advancedTeams;
    private String message;
}
