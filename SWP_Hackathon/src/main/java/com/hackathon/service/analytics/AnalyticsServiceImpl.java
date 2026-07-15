package com.hackathon.service.analytics;

import com.hackathon.dto.analytics.MetricResultDTO;
import com.hackathon.dto.analytics.RawScoreDTO;
import com.hackathon.dto.analytics.ReliabilityResultDTO;
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

    // =========================================================
    // ĐÁNH GIÁ ĐỘ TIN CẬY (ICC & Cronbach's Alpha)
    // =========================================================
    @Override
    public ReliabilityResultDTO calculateReliabilityMetrics(Integer eventId) {
        List<RawScoreDTO> rawScores = evaluationDetailRepository.fetchRawScoresByEventId(eventId);
        if (rawScores.isEmpty()) {
            return ReliabilityResultDTO.builder().totalEvaluations(0).build();
        }

        double alpha = calculateCronbachAlpha(rawScores);
        double icc = calculateICC(rawScores);

        return ReliabilityResultDTO.builder()
                .eventId(eventId)
                .totalEvaluations(rawScores.size())
                .cronbachAlpha(ReliabilityResultDTO.MetricDetail.builder()
                        .value(Math.round(alpha * 1000.0) / 1000.0)
                        .interpretation(interpretAlpha(alpha))
                        .build())
                .icc(ReliabilityResultDTO.MetricDetail.builder()
                        .value(Math.round(icc * 1000.0) / 1000.0)
                        .interpretation(interpretICC(icc))
                        .build())
                .build();
    }

    private double calculateCronbachAlpha(List<RawScoreDTO> rawScores) {
        Map<Integer, List<RawScoreDTO>> scoresByCriteria = rawScores.stream()
                .collect(Collectors.groupingBy(RawScoreDTO::getCriterionId));
        int k = scoresByCriteria.size();
        if (k <= 1) return 0.0;

        double sumOfItemVariances = scoresByCriteria.values().stream()
                .mapToDouble(list -> calculateVariance(list.stream().mapToDouble(dto -> dto.getScore().doubleValue()).toArray()))
                .sum();

        Map<String, Double> totalScoresByEvaluation = rawScores.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getJudgeId() + "-" + dto.getTeamId(),
                        Collectors.summingDouble(dto -> dto.getScore().doubleValue())
                ));
        double varianceOfTotalScores = calculateVariance(totalScoresByEvaluation.values().stream().mapToDouble(Double::doubleValue).toArray());

        if (varianceOfTotalScores == 0) return 0.0;
        return ((double) k / (k - 1)) * (1 - (sumOfItemVariances / varianceOfTotalScores));
    }

    private double calculateICC(List<RawScoreDTO> rawScores) {
        Map<Integer, List<Double>> scoresByTeam = rawScores.stream()
                .collect(Collectors.groupingBy(
                        RawScoreDTO::getTeamId,
                        Collectors.mapping(dto -> dto.getScore().doubleValue(), Collectors.toList())
                ));

        int n = scoresByTeam.size();
        if (n <= 1) return 0.0;

        long totalScoresCount = scoresByTeam.values().stream().mapToLong(List::size).sum();
        double k = (double) totalScoresCount / n;
        double grandMean = rawScores.stream().mapToDouble(dto -> dto.getScore().doubleValue()).average().orElse(0.0);

        double ssb = scoresByTeam.values().stream().mapToDouble(list -> {
            double teamMean = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            return list.size() * Math.pow(teamMean - grandMean, 2);
        }).sum();
        double msb = ssb / (n - 1);

        double ssw = scoresByTeam.values().stream().mapToDouble(list -> {
            double teamMean = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            return list.stream().mapToDouble(score -> Math.pow(score - teamMean, 2)).sum();
        }).sum();

        long dfWithin = totalScoresCount - n;
        double msw = dfWithin > 0 ? ssw / dfWithin : 0;

        if ((msb + (k - 1) * msw) == 0) return 0.0;
        return (msb - msw) / (msb + (k - 1) * msw);
    }

    private double calculateVariance(double[] values) {
        if (values.length <= 1) return 0.0;
        double mean = Arrays.stream(values).average().orElse(0.0);
        return Arrays.stream(values).map(v -> Math.pow(v - mean, 2)).sum() / (values.length - 1);
    }

    private String interpretAlpha(double alpha) {
        if (alpha >= 0.9) return "Excellent";
        if (alpha >= 0.8) return "Good";
        if (alpha >= 0.7) return "Acceptable";
        if (alpha >= 0.6) return "Questionable";
        if (alpha >= 0.5) return "Poor";
        return "Unacceptable";
    }

    private String interpretICC(double icc) {
        if (icc >= 0.9) return "Excellent";
        if (icc >= 0.75) return "Good";
        if (icc >= 0.5) return "Moderate";
        return "Poor";
    }

    
}