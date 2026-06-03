package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class RoundDTO {
    private Integer roundId;
    private String advancementRule;
    private LocalDateTime endTime;
    private String roundName;
    private LocalDateTime startTime;
    private Integer criteriaSetId;
    private Integer eventId;

}
