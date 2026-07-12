package com.hackathon.dto.participant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Setter
@Getter
@Builder
public class RoundParticipantDetailDTO {
    Integer roundId;
    String roundName;
    List<CategoryParticipantDTO> categories;
}
