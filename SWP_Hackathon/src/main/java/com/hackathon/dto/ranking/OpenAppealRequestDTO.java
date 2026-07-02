package com.hackathon.dto.ranking;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class OpenAppealRequestDTO {
    private Integer roundId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
