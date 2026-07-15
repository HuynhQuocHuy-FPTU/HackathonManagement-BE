package com.hackathon.dto.analytics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReliabilityResultDTO {
    private Integer eventId;
    private long totalEvaluations;
    private MetricDetail cronbachAlpha;
    private MetricDetail icc;

    @Data
    @Builder
    public static class MetricDetail {
        private double value;
        private String interpretation;
    }
}