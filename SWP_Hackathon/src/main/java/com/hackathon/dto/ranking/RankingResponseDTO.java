package com.hackathon.dto.ranking;

import com.hackathon.entity.enums.ParticipantStatus;
import com.hackathon.entity.enums.RoundStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RankingResponseDTO {
    private Integer participantId;
    private BigDecimal totalScore;
    private Integer rank;
    private String teamName;
    private ParticipantStatus status;


}
