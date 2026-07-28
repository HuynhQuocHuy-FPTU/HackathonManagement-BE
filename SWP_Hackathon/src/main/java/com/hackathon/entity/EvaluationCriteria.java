package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.CriteriaType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "EvaluationCriteria")
@Entity

public class EvaluationCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Evaluation_Criteria_ID")
    private int evaluationCriteriaId;
    @Column(name = "Criteria_Name", columnDefinition = "NVARCHAR(255)", nullable = true)
    private String criteriaName;
    @Column(name = "Weight", precision = 10, scale = 2,nullable = true)
    private BigDecimal weight;
    @Column(name = "Description", columnDefinition = "NVARCHAR(1000)", nullable = true)
    private String description;
    @Column(name = "Max_Score", nullable = true)
    private int maxScore;

    @Column(name = "Criteria_Type", nullable = true)
    @Enumerated(EnumType.STRING)
    private CriteriaType type;

    // 1 evaluationCriteria  - N evaluation detail
    @OneToMany(mappedBy = "evaluationCriteria", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<EvaluationDetail> evaluationDetails = new ArrayList<>();

    // 1 round - N evaluationCriteria
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Round_ID", nullable = false)
    private Round round;

}

