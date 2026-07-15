package com.hackathon.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RawScoreDTO {
    private Integer eventId;
    private Integer roundId;
    private Integer categoryId;
    private Integer judgeId;
    private Integer teamId;
    private Integer criterionId;
    private String criterionName;
    private BigDecimal score;
}