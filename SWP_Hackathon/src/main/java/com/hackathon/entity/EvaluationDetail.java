package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Data
@Entity
@Table(name="EvaluationDetail")
public class EvaluationDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Evaluation_Detail_ID")
    private int evaluationDetailId;
    @Column(name="Score", precision = 10 , scale = 2, nullable = false)
    private BigDecimal score;
    @Column(name = "Comment", columnDefinition = "NVARCHAR(500)")
    private String comment;

    // 1 Criteria Detail - N Evaluation _ Detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Criteria_ID", nullable = false)
     private CriteriaDetail criteriaDetail;

    // 1 evaluationCriteria  - N evaluation detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="Evaluation_Criteria_ID", nullable = false)
    private EvaluationCriteria evaluationCriteria;
}
