package com.hackathon.dto.submission;

import com.hackathon.entity.enums.SubmissionStatus;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ResultSubmissionResponse {
    private Integer submissionId;
    private BigDecimal totalScore;
    private String comment;
    private Integer rank;
    private SubmissionStatus submissionStatus;
}
