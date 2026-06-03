package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class TeamMemberDTO {
    private Integer id;
    private Boolean role;
    private Integer studentId;
    private Integer teamId;


}
