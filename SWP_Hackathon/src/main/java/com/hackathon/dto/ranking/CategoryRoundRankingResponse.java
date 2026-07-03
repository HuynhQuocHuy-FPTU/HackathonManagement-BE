package com.hackathon.dto.ranking;


import com.hackathon.dto.ParticipantResponseDTO;
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
    private ApprovalSummary approvalSummary;
    private List<CategoryRankingResponse> categoriesRanking;
    private List<ParticipantResponseDTO> teamsResult;

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Builder
    public static class ApprovalSummary {
        private int totalTeamsProcessed;
        private int totalPassed;
        private int totalFailed;
    }
}
