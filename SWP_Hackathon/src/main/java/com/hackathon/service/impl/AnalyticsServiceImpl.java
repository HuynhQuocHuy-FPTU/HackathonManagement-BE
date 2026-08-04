package com.hackathon.service.impl;

import com.hackathon.dto.analytics.MetricResultDTO;
import com.hackathon.dto.analytics.RawScoreDTO;
import com.hackathon.dto.analytics.ReliabilityResultDTO;
import com.hackathon.repository.EvaluationDetailRepository;
import com.hackathon.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hiện thực dịch vụ Phân tích dữ liệu (Analytics Service) cho mô hình Research-Based Learning (RBL).
 * Lớp này chịu trách nhiệm xử lý các thuật toán thống kê mô tả, đo lường độ tin cậy của bộ tiêu chí
 * và mã hóa ẩn danh dữ liệu nghiên cứu khoa học.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final EvaluationDetailRepository evaluationDetailRepository;

    // =========================================================================
    // 0. SCOPE RESOLVER (BỘ ĐIỀU HƯỚNG PHẠM VI DỮ LIỆU)
    // =========================================================================
    /**
     * Hàm helper private dùng chung để định tuyến câu query xuống DB dựa trên phạm vi (scope).
     */
    private List<RawScoreDTO> fetchDataByScope(String scope, Integer id) {
        switch (scope.toLowerCase()) {
            case "event": return evaluationDetailRepository.fetchRawScoresByEventId(id);
            case "round": return evaluationDetailRepository.fetchRawScoresByRoundId(id);
            case "category": return evaluationDetailRepository.fetchRawScoresByCategoryRoundId(id);
            case "submission": return evaluationDetailRepository.fetchRawScoresBySubmissionId(id);
            default: throw new IllegalArgumentException("Phạm vi dữ liệu (scope) không hợp lệ.");
        }
    }

    // =========================================================================
    // API 1: THỐNG KÊ MÔ TẢ THEO TIÊU CHÍ (CRITERIA STATS)
    // =========================================================================
    @Override
    public List<MetricResultDTO> getCriteriaStats(String scope, Integer id) {
        // 1. Kéo tập dữ liệu điểm thô từ Database lên RAM thông qua DTO Projection
        List<RawScoreDTO> rawScores = fetchDataByScope(scope, id);
        if (rawScores.isEmpty()) return Collections.emptyList();

        // 2. Gom nhóm tập điểm số theo Tên tiêu chí (Criteria Name)
        Map<String, List<RawScoreDTO>> groupedData = rawScores.stream()
                .collect(Collectors.groupingBy(RawScoreDTO::getCriterionName));

        // 3. Duyệt qua từng nhóm tiêu chí và thực hiện tính toán các chỉ số toán học
        return groupedData.entrySet().stream().map(entry -> {
            String criterionName = entry.getKey();

            // Ép kiểu tập điểm BigDecimal sang mảng double nguyên thủy để tối ưu tốc độ tính toán
            double[] scoreValues = entry.getValue().stream()
                    .mapToDouble(dto -> dto.getScore().doubleValue())
                    .toArray();

            return calculateMetrics(criterionName, scoreValues);
        }).collect(Collectors.toList());
    }

    /**
     * Thuật toán Thống kê mô tả (Descriptive Statistics)
     */
    private MetricResultDTO calculateMetrics(String groupKey, double[] scores) {
        // Sử dụng DoubleSummaryStatistics của Java 8 để tính nhanh Mean, Min, Max, Count
        DoubleSummaryStatistics stats = Arrays.stream(scores).summaryStatistics();
        double mean = stats.getAverage();
        long count = stats.getCount();

        // Tính Phương sai (Variance): Trung bình của bình phương độ lệch
        double variance = 0;
        if (count > 1) {
            double sumOfSquaredDiffs = Arrays.stream(scores).map(s -> Math.pow(s - mean, 2)).sum();
            variance = sumOfSquaredDiffs / (count - 1); // Chia cho (n-1) để lấy phương sai mẫu (Sample Variance)
        }

        // Tính Độ lệch chuẩn (Standard Deviation)
        double stdDev = Math.sqrt(variance);

        // Đóng gói DTO và làm tròn 2 chữ số thập phân cho đẹp UI
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

    // =========================================================================
    // API 2: ĐÁNH GIÁ ĐỘ TIN CẬY HỆ THỐNG (RELIABILITY ANALYTICS)
    // =========================================================================
    @Override
    public ReliabilityResultDTO calculateReliabilityMetrics(Integer eventId) {
        // 1. Lấy toàn bộ dữ liệu chấm điểm của cả Sự kiện
        List<RawScoreDTO> rawScores = evaluationDetailRepository.fetchRawScoresByEventId(eventId);
        if (rawScores.isEmpty()) {
            return ReliabilityResultDTO.builder().totalEvaluations(0).build();
        }

        // 2. Phân rã luồng tính toán cho 2 chỉ số độc lập
        double alpha = calculateCronbachAlpha(rawScores);
        double icc = calculateICC(rawScores);

        // 3. Đóng gói kết quả kèm theo diễn giải (Interpretation) hỗ trợ Frontend hiển thị trực quan
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

    /**
     * Thuật toán Cronbach's Alpha đo tính nhất quán của bộ Tiêu chí chấm điểm.
     */
    private double calculateCronbachAlpha(List<RawScoreDTO> rawScores) {
        // 1. Tính k (Tổng số lượng tiêu chí được dùng để chấm)
        Map<Integer, List<RawScoreDTO>> scoresByCriteria = rawScores.stream()
                .collect(Collectors.groupingBy(RawScoreDTO::getCriterionId));
        int k = scoresByCriteria.size();
        if (k <= 1) return 0.0; // Phải có từ 2 tiêu chí trở lên mới so sánh được độ nhất quán

        // 2. Tính Tổng phương sai của từng tiêu chí (Sum of Item Variances)
        double sumOfItemVariances = scoresByCriteria.values().stream()
                .mapToDouble(list -> calculateVariance(list.stream().mapToDouble(dto -> dto.getScore().doubleValue()).toArray()))
                .sum();

        // 3. Tính Phương sai của Tổng điểm (Variance of Total Scores)
        // Gom nhóm theo Phiếu chấm (1 Giám khảo chấm 1 Đội -> Ra 1 Phiếu tổng điểm)
        Map<String, Double> totalScoresByEvaluation = rawScores.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getJudgeId() + "-" + dto.getTeamId(),
                        Collectors.summingDouble(dto -> dto.getScore().doubleValue())
                ));
        double varianceOfTotalScores = calculateVariance(totalScoresByEvaluation.values().stream().mapToDouble(Double::doubleValue).toArray());

        if (varianceOfTotalScores == 0) return 0.0;

        // 4. Ráp vào công thức chuẩn của Cronbach's Alpha
        return ((double) k / (k - 1)) * (1 - (sumOfItemVariances / varianceOfTotalScores));
    }

    /**
     * Thuật toán ICC (Intraclass Correlation Coefficient) đo độ đồng thuận của Ban giám khảo.
     * Sử dụng mô hình One-Way ANOVA để bóc tách sai số.
     */
    private double calculateICC(List<RawScoreDTO> rawScores) {
        // 1. Gom nhóm điểm theo từng Đội thi (Để xem các giám khảo chấm 1 đội có giống nhau không)
        Map<Integer, List<Double>> scoresByTeam = rawScores.stream()
                .collect(Collectors.groupingBy(
                        RawScoreDTO::getTeamId,
                        Collectors.mapping(dto -> dto.getScore().doubleValue(), Collectors.toList())
                ));

        int n = scoresByTeam.size(); // Số lượng đội thi
        if (n <= 1) return 0.0;

        long totalScoresCount = scoresByTeam.values().stream().mapToLong(List::size).sum();
        double k = (double) totalScoresCount / n; // Trung bình số giám khảo chấm cho mỗi đội

        // Điểm trung bình cộng của toàn bộ sự kiện
        double grandMean = rawScores.stream().mapToDouble(dto -> dto.getScore().doubleValue()).average().orElse(0.0);

        // 2. Tính MSB (Mean Square Between) - Sự chênh lệch năng lực thực sự giữa các Đội thi
        double ssb = scoresByTeam.values().stream().mapToDouble(list -> {
            double teamMean = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            return list.size() * Math.pow(teamMean - grandMean, 2);
        }).sum();
        double msb = ssb / (n - 1);

        // 3. Tính MSW (Mean Square Within) - Độ lệch điểm do sai số nội bộ (Giám khảo chấm lệch pha)
        double ssw = scoresByTeam.values().stream().mapToDouble(list -> {
            double teamMean = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            return list.stream().mapToDouble(score -> Math.pow(score - teamMean, 2)).sum();
        }).sum();

        long dfWithin = totalScoresCount - n;
        double msw = dfWithin > 0 ? ssw / dfWithin : 0;

        if ((msb + (k - 1) * msw) == 0) return 0.0;

        // 4. Ráp vào công thức ICC (Loại bỏ MSW để tìm ra độ tin cậy thực)
        return (msb - msw) / (msb + (k - 1) * msw);
    }

    private double calculateVariance(double[] values) {
        if (values.length <= 1) return 0.0;
        double mean = Arrays.stream(values).average().orElse(0.0);
        return Arrays.stream(values).map(v -> Math.pow(v - mean, 2)).sum() / (values.length - 1);
    }

    private String interpretAlpha(double alpha) {
        if (alpha >= 0.9) return "Excellent (Rất đồng nhất)";
        if (alpha >= 0.8) return "Good (Đồng nhất tốt)";
        if (alpha >= 0.7) return "Acceptable (Chấp nhận được)";
        if (alpha >= 0.6) return "Questionable (Đáng lo ngại)";
        if (alpha >= 0.5) return "Poor (Kém)";
        return "Unacceptable (Không thể chấp nhận)";
    }

    private String interpretICC(double icc) {
        if (icc >= 0.9) return "Excellent (Rất đồng thuận)";
        if (icc >= 0.75) return "Good (Đồng thuận tốt)";
        if (icc >= 0.5) return "Moderate (Đồng thuận trung bình)";
        return "Poor (Bất đồng quan điểm)";
    }

    // =========================================================================
    // API 3: XUẤT TỆP CSV (EXPORT DATA)
    // =========================================================================
    @Override
    public byte[] exportAnonymizedCsv(String scope, Integer id) {
        // 1. Tận dụng lại hàm fetch data
        List<RawScoreDTO> rawScores = fetchDataByScope(scope, id);
        if (rawScores.isEmpty()) return new byte[0];

        // 2. Tạo từ điển ẩn danh (Local Maps). Maps này sẽ bị Hủy ngay sau khi xuất file xong để bảo mật danh tính
        Map<Integer, String> judgeAnonymizer = new HashMap<>();
        Map<Integer, String> teamAnonymizer = new HashMap<>();

        // 3. Dùng StringBuilder để tối ưu bộ nhớ thay vì cộng chuỗi String thông thường
        StringBuilder csvBuilder = new StringBuilder();

        // Thêm BOM (Byte Order Mark) để định dạng UTF-8, giúp file mở bằng Excel không bị lỗi font Tiếng Việt
        csvBuilder.append('\ufeff');
        csvBuilder.append("Submission_Code,Judge_Code,Criterion_Name,Score,Round_ID\n");

        // 4. Map từng dòng dữ liệu và gắn mã ẩn danh
        for (RawScoreDTO score : rawScores) {
            String teamCode = anonymize(score.getTeamId(), teamAnonymizer, "Team_");
            String judgeCode = anonymize(score.getJudgeId(), judgeAnonymizer, "Judge_");

            csvBuilder.append(teamCode).append(",")
                    .append(judgeCode).append(",")
                    .append("\"").append(score.getCriterionName()).append("\",") // Đặt trong ngoặc kép tránh lỗi nếu tên tiêu chí có dấu phẩy
                    .append(score.getScore()).append(",")
                    .append(score.getRoundId())
                    .append("\n");
        }

        // 5. Stream trực tiếp chuỗi ra mảng byte để Controller download thẳng xuống Client
        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Hàm helper xử lý cấp phát mã ẩn danh tự động
     */
    private String anonymize(Integer originalId, Map<Integer, String> dict, String prefix) {
        return dict.computeIfAbsent(originalId, id -> prefix + (dict.size() + 1));
    }
}