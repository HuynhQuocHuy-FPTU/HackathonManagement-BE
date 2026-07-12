package com.hackathon.dto.participant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@Builder
public class CategoryParticipantDTO {
    private String roundName;
    private Integer roundIndex;
    private Integer categoryRoundId;
    private String categoryName;
    private Integer totalTeams;
    List<ParticipantDetailDTO> participants;
}
