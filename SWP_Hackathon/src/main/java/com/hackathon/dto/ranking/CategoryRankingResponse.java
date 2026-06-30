package com.hackathon.dto.ranking;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CategoryRankingResponse {
    private Integer categoryRoundId;
    private Integer categoryId;
    private String categoryName;
    private List<RankingResponseDTO> teams;
}
