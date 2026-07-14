package com.hackathon.dto.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class AdvancedTeamDTO {
    private Integer teamId;
    private String teamName;
    private BigDecimal totalScore;
    private Integer rank;
    private Integer newTeamParticipantId;
}
