package com.hackathon.dto.participant;

import com.hackathon.dto.round.RoundStatusDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
public class CurrentParticipantDTO {
    private Integer eventID;
    private String eventName;
    private Integer categoryId;
    private String categoryName;
    private String teamName;
    private List<RoundStatusDTO> rounds;
}
