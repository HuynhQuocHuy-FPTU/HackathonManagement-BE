package com.hackathon.service.ranking;

import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.dto.ranking.OpenAppealRequestDTO;
import com.hackathon.security.CustomUserDetails;

public interface RankingService {
    CategoryRoundRankingResponse getRankingByEventCoordinator(Integer roundId, CustomUserDetails userDetails);

    CategoryRoundRankingResponse approveRanking(CustomUserDetails userDetails, Integer roundId);

    CategoryRoundRankingResponse rejectRanking(CustomUserDetails userDetails, Integer roundId);

    void publishDraftRanking(Integer roundId, CustomUserDetails userDetails);

    void publishFinalRanking(Integer roundId, CustomUserDetails userDetails);

    void openAppeals(CustomUserDetails userDetails, OpenAppealRequestDTO request);

    CategoryRoundRankingResponse getTopNRanking(Integer roundId);

    CategoryRoundRankingResponse getRankingByAll(Integer roundId, CustomUserDetails userDetails);

}
