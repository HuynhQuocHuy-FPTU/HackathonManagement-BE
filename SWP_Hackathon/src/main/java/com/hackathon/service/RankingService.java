package com.hackathon.service;

import com.hackathon.dto.ranking.CategoryRoundRankingResponse;
import com.hackathon.security.CustomUserDetails;

public interface RankingService {
    CategoryRoundRankingResponse getRankingByEventCoordinator(Integer roundId, CustomUserDetails userDetails);

    void publishDraftRankingAndOpenAppeals(Integer roundId, CustomUserDetails userDetails, Integer minutesAmount);

    void publishFinalRanking(Integer roundId , CustomUserDetails userDetails);

    CategoryRoundRankingResponse getTopNRanking(Integer roundId);

    CategoryRoundRankingResponse getRankingByAll(Integer roundId, CustomUserDetails userDetails);

    String getRankingPublicExcels(Integer roundId, String type);

}
