package com.hackathon.service.analytics;

import com.hackathon.dto.analytics.MetricResultDTO;
import com.hackathon.dto.analytics.RawScoreDTO;
import com.hackathon.repository.EvaluationDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final EvaluationDetailRepository evaluationDetailRepository;

    // =========================================================
    // 0. SCOPE RESOLVER (Private Helper)
    // =========================================================
    private List<RawScoreDTO> fetchDataByScope(String scope, Integer id) {
        switch (scope.toLowerCase()) {
            case "event": return evaluationDetailRepository.fetchRawScoresByEventId(id);
            case "round": return evaluationDetailRepository.fetchRawScoresByRoundId(id);
            case "category": return evaluationDetailRepository.fetchRawScoresByCategoryRoundId(id);
            case "submission": return evaluationDetailRepository.fetchRawScoresBySubmissionId(id);
            default: throw new IllegalArgumentException("Invalid scope.");
        }
    }

    // =========================================================
    // 1. THỐNG KÊ MÔ TẢ THEO TIÊU CHÍ
    // =========================================================
    @Override
    public List<MetricResultDTO> getCriteriaStats(String scope, Integer id) {
        List<RawScoreDTO> rawScores = fetchDataByScope(scope, id);
        if (rawScores.isEmpty()) return Collections.emptyList();

        Map<String, List<RawScoreDTO>> groupedData = rawScores.stream()
                .collect(Collectors.groupingBy(RawScoreDTO::getCriterionName));

        return groupedData.entrySet().stream().map(entry -> {
            String criterionName = entry.getKey();
            double[] scoreValues = entry.getValue().stream()
                    .mapToDouble(dto -> dto.getScore().doubleValue())
                    .toArray();
            return calculateMetrics(criterionName, scoreValues);
        }).collect(Collectors.toList());
    }

    private MetricResultDTO calculateMetrics(String groupKey, double[] scores) {
        DoubleSummaryStatistics stats = Arrays.stream(scores).summaryStatistics();
        double mean = stats.getAverage();
        long count = stats.getCount();

        double variance = 0;
        if (count > 1) {
            double sumOfSquaredDiffs = Arrays.stream(scores).map(s -> Math.pow(s - mean, 2)).sum();
            variance = sumOfSquaredDiffs / (count - 1);
        }
        double stdDev = Math.sqrt(variance);

        return MetricResultDTO.builder()
                .groupByTarget(groupKey)
                .countEvaluations(count)
                .mean(Math.round(mean * 100.0) / 100.0)
                .variance(Math.round(variance * 100.0) / 100.0)
                .standardDeviation(Math.round(stdDev * 100.0) / 100.0)
                .min(stats.getMin())
                .max(stats.getMax())
                .build();
    }
}