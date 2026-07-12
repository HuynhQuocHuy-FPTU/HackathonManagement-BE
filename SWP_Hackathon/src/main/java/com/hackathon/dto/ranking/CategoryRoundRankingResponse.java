package com.hackathon.dto.ranking;


import com.hackathon.dto.participant.ParticipantResponseDTO;
import com.hackathon.entity.enums.RoundStatus;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CategoryRoundRankingResponse {
    private Integer roundId;
    private String roundName;
    private Integer orderIndex;
    private String advancementRule;
    private Integer topN;
    private RoundStatus roundStatus;
    private List<CategoryRankingResponse> categoriesRanking;
    private List<ParticipantResponseDTO> teamsResult;
    private String draftExcelUrl;

}
