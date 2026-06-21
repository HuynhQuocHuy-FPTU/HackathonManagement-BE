package com.hackathon.dto;

import com.hackathon.entity.enums.ParticipantStatus;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class ParticipantResponseDTO {
    private Integer participantId;
    private String teamName;
    private double totalScore;
    private Integer rank;
    private ParticipantStatus status;
}
