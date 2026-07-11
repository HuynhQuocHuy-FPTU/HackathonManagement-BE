package com.hackathon.dto.team;

import com.hackathon.dto.round.RoundStatusDTO;
import com.hackathon.entity.enums.ParticipantStatus;
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
