package com.hackathon.dto.team;


import com.hackathon.entity.Category;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TeamCompetitionResponse {
    private String eventName;
    private Integer teamId;
    private String teamName;
    private List<RoundInfo> rounds;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoundInfo {
        private String roundName;
        private List<Category> categories;
    }
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Category {
        private String categoryName;
    }

}
