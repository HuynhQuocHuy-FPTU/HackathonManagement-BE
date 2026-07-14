package com.hackathon.service.grading.support;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

@Component
public class ScoreStatisticsUtil {
    public BigDecimal average(List<BigDecimal> values) {
        if (values == null || values.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Độ lệch chuẩn mẫu (sample standard deviation, dùng n-1) — trả về 0 nếu < 2 giá trị
     * (không đủ dữ liệu để có ý nghĩa thống kê).
     */
    public BigDecimal standardDeviation(List<BigDecimal> values, BigDecimal mean) {
        if (values == null || values.size() < 2) return BigDecimal.ZERO;

        BigDecimal sumSquaredDiff = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(values.size() - 1), MathContext.DECIMAL64);
        return variance.sqrt(MathContext.DECIMAL64).setScale(2, RoundingMode.HALF_UP);
    }
}
