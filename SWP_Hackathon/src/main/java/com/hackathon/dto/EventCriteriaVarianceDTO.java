package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventCriteriaVarianceDTO {
    private Integer eventId;
    private String eventName;
    private List<RoundVarianceDTO> rounds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundVarianceDTO {
        private Integer roundId;
        private String roundName;
        private Integer orderIndex;
        private List<CategoryVarianceDTO> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryVarianceDTO {
        private Integer categoryRoundId;
        private Integer categoryId;
        private String categoryName;
        private List<CriteriaVarianceDTO> criteria;
    }
}
