package com.hackathon.dto.participant;

import com.hackathon.entity.enums.ExpertRole;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class ExpertAssignedGroupDTO {
    private Integer categoryRoundId;
    private Integer categoryId;
    private String categoryName;
    private Integer roundId;
    private String roundName;
    private ExpertRole role;
    private List<ParticipantResponseDTO> participants;
}
