package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity(name = "Criteria_Round")
public class CriteriaRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Criteria_ID")
    private int criteriaId;
    @Column(name = "Criteria_Name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String criteriaName;
    @Column(name = "Weight", precision = 10 , scale = 2, nullable = false )
    private BigDecimal weight;


    // 1 Criteria Round - N Evaluation _ Detail
    @OneToMany(mappedBy = "criteriaRound", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<EvaluationDetail> evaluationDetail = new ArrayList<>() ;

    // N Criteria Round - 1 Criteria Detail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Criteria_Detail_ID", nullable = false)
    private CriteriaDetail criteriaDetail;

    //1 Round - N Criteria_Round
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Round_ID", nullable = false)
    private Round round;

}
