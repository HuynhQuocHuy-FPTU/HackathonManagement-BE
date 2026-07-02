package com.hackathon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamAppealRequestDTO {
    private Integer roundId;
    private String requestMessage;
}
