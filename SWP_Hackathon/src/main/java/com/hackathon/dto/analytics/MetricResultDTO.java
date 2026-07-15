package com.hackathon.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricResultDTO {
    private String groupByTarget;
    private long countEvaluations;
    private double mean;
    private double variance;
    private double standardDeviation;
    private double min;
    private double max;
}