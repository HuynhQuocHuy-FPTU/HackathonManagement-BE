package com.hackathon.dto.participant;

import com.hackathon.entity.enums.ParticipantStatus;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class ParticipantResponseDTO {
    private Integer participantId;
    private String teamName;
    private BigDecimal totalScore;
    private Integer rank;
    private ParticipantStatus status;

}
