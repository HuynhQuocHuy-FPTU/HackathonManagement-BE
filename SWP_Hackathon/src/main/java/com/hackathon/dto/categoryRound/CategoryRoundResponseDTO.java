package com.hackathon.dto.categoryRound;

import com.hackathon.entity.enums.ExpertRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
public class CategoryRoundResponseDTO {
    private Integer roundId;
    private String roundName;
    private LocalDateTime roundDate;
    private LocalDateTime roundEnd;
    private Integer categoryRoundId;
    private Integer categoryId;
    private String categoryName;
    private ExpertRole role;
}
