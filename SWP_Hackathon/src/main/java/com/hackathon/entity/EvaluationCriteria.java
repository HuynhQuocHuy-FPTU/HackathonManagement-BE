package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
// Bang nay duoc dung de chinh sua cac tieu chi danh gia cho moi round
public class EvaluationCriteria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Evaluation_Criteria_ID")
    private int evaluationCriteriaId;
    @Column(name = "Criteria_Name", columnDefinition = "NVARCHAR(255)", nullable = false)
    private String criteriaName;
    @Column(name = "Weight", precision = 10, scale = 2, nullable = false)
    private BigDecimal weight;
    @Column(name = "Description", columnDefinition = "NVARCHAR(1000)")
    private String description;


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "Criteria_Detail_ID", nullable = false)
//    private CriteriaDetail criteriaDetail; // Lưu ID gốc từ bảng EvaluationDetail sang

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

