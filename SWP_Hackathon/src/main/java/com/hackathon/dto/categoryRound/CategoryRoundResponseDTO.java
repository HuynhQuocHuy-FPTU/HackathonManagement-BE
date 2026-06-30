package com.hackathon.dto.categoryRound;

import com.hackathon.entity.enums.ExpertRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class CategoryRoundResponseDTO {
    private Integer roundId;
    private String roundName;
    private Integer categoryRoundId;
    private Integer categoryId;
    private String categoryName;
    private ExpertRole role;
}
