package com.hackathon.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="Evaluation")
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="Evaluation_ID")
    private int evaluationId;
    @Column(name="Total_Score", precision = 10 , scale = 2, nullable = false)
    private BigDecimal score;
    @Column(name = "Comment", columnDefinition = "NVARCHAR(500)")
    private String comment;

    //1 expertAsgin -N evaluation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Expert_ID", nullable = false)
    private ExpertAssign expertAssign;

    // 1 Submission -N EVALUATION
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Submission_ID", nullable = false)
    private Submission submission;

    // 1 Evaluation - N Evaluation Detail
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<EvaluationDetail> evaluationDetails= new ArrayList<>();
}
