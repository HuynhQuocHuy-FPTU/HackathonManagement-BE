package com.hackathon.service.impl;

import com.hackathon.dto.analytics.MetricResultDTO;
import com.hackathon.dto.analytics.RawScoreDTO;
import com.hackathon.dto.analytics.ReliabilityResultDTO;
import com.hackathon.dto.analytics.StudentCountResponse;
import com.hackathon.repository.EvaluationDetailRepository;
import com.hackathon.repository.StudentRepository;
import com.hackathon.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// Phân tích dữ liệu điểm để phục vụ thống kê, nghiên cứu và đánh giá độ tin cậy khi chấm thi.
// Các kết quả xuất ra được tổng hợp hoặc ẩn danh để không làm lộ danh tính đội và giám khảo.
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final EvaluationDetailRepository evaluationDetailRepository;
    private final StudentRepository studentRepository;

    // Chọn câu truy vấn phù hợp với phạm vi dữ liệu mà người dùng yêu cầu.
    private List<RawScoreDTO> fetchDataByScope(String scope, Integer id) {
        // Chuẩn hóa phạm vi thành chữ thường để việc so sánh không phụ thuộc cách nhập hoa hay thường.
        switch (scope.toLowerCase()) {
            // Lấy điểm của toàn bộ sự kiện.
            case "event": return evaluationDetailRepository.fetchRawScoresByEventId(id);
            // Lấy điểm trong một vòng thi.
            case "round": return evaluationDetailRepository.fetchRawScoresByRoundId(id);
            // Lấy điểm trong một danh mục của vòng.
            case "category": return evaluationDetailRepository.fetchRawScoresByCategoryRoundId(id);
            // Lấy điểm của một bài nộp cụ thể.
            case "submission": return evaluationDetailRepository.fetchRawScoresBySubmissionId(id);
            // Từ chối giá trị ngoài các phạm vi được hệ thống hỗ trợ.
            default: throw new IllegalArgumentException("Phạm vi dữ liệu (scope) không hợp lệ.");
        }
    }

    // Tính các chỉ số thống kê cơ bản của điểm theo từng tiêu chí.
    @Override
    public List<MetricResultDTO> getCriteriaStats(String scope, Integer id) {
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

    // Tính số lượng, trung bình, phương sai, độ lệch chuẩn, giá trị nhỏ nhất và lớn nhất.
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

    // Đánh giá tính nhất quán của tiêu chí và mức đồng thuận giữa các giám khảo.
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

    // Tính hệ số Cronbach Alpha để đo mức nhất quán của bộ tiêu chí chấm điểm.
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

    // Tính hệ số tương quan nội lớp để đo mức đồng thuận giữa các giám khảo.
    // Phép tính tách chênh lệch giữa các đội khỏi sai số chấm bên trong cùng một đội.
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

    // Diễn giải hệ số nhất quán tiêu chí thành mức độ dễ hiểu cho người xem.
    private String interpretAlpha(double alpha) {
        if (alpha >= 0.9) return "Excellent (Rất đồng nhất)";
        if (alpha >= 0.8) return "Good (Đồng nhất tốt)";
        if (alpha >= 0.7) return "Acceptable (Chấp nhận được)";
        if (alpha >= 0.6) return "Questionable (Đáng lo ngại)";
        if (alpha >= 0.5) return "Poor (Kém)";
        return "Unacceptable (Không thể chấp nhận)";
    }

    // Diễn giải hệ số đồng thuận thành mức đánh giá giữa các giám khảo.
    private String interpretICC(double icc) {
        if (icc >= 0.9) return "Excellent (Rất đồng thuận)";
        if (icc >= 0.75) return "Good (Đồng thuận tốt)";
        if (icc >= 0.5) return "Moderate (Đồng thuận trung bình)";
        return "Poor (Bất đồng quan điểm)";
    }

    // Xuất dữ liệu điểm đã ẩn danh thành tệp CSV dùng cho nghiên cứu.
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

    @Override
    public StudentCountResponse getStudentCounts() {
        // 1. Lấy tổng số lượng sinh viên (Hàm count() mặc định của JpaRepository)
        long totalStudents = studentRepository.count();

        // 2. Lấy số lượng SV FPT (Email FPT hoặc Tên trường có chữ FPT)
        long fptStudents = studentRepository.countFptStudents();

        // 3. Tính SV trường ngoài bằng phép trừ
        long externalStudents = totalStudents - fptStudents;

        return StudentCountResponse.builder()
                .fptStudentCount(fptStudents)
                .externalStudentCount(externalStudents)
                .build();
    }

    // Cấp mã thay thế ổn định cho một định danh trong phạm vi lần xuất dữ liệu hiện tại.
    private String anonymize(Integer originalId, Map<Integer, String> dict, String prefix) {
        return dict.computeIfAbsent(originalId, id -> prefix + (dict.size() + 1));
    }
}
