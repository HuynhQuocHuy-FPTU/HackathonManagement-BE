package com.hackathon.dto.expert;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ExpertOverviewResponse {
    private long totalAssigned;
    private long completedReviews;
    private long pendingReviews;
    private long reEvaluationReviews;
}
