package com.hackathon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaVarianceDTO {
        private Integer evaluationCriteriaId;
        private String criteriaName;

        private BigDecimal overallMean;       // trung bình của các judgeMean (không phải trung bình điểm thô)
        private BigDecimal variance;          // trung bình phương sai giữa giám khảo trên cùng bài
        private BigDecimal standardDeviation;
        private int comparedSubmissionCount;

        private List<JudgeMeanDTO> judgeMeans;
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class JudgeMeanDTO {
            private Integer expertId;
            private String expertName;
            private BigDecimal judgeMean;              // điểm trung bình của giám khảo này cho tiêu chí này
            private BigDecimal deviationFromOverall;    // judgeMean - overallMean (âm = chấm khắt khe hơn mặt bằng, dương = dễ hơn)
            private int submissionCount;                // số bài giám khảo này đã chấm cho tiêu chí này
        }
    }
