package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "EvaluationDetail")
public class EvaluationDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Evaluation_Detail_ID")
    private int id;
    @Column(name = "Score", precision = 10, scale = 2)
    private BigDecimal score;
    @Column(name = "Comment", columnDefinition = "NVARCHAR(500)")
    private String comment;

    @Column(name="Is_ReEvaluation")
    private Boolean isReEvaluation;
    // 1 evaluation - N evaluation detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Evaluation_ID", nullable = false)
    private Evaluation evaluation;

    // 1 evaluationCriteria - N evaluation_detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Evaluation_Criteria_ID", nullable = true)
    private EvaluationCriteria evaluationCriteria;

}
