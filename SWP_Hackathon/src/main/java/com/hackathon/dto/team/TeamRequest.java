package com.hackathon.dto.team;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeamRequest {
    private Integer eventId;
    private Integer teamId;
    private String teamName;
    private String studentCode;
}
