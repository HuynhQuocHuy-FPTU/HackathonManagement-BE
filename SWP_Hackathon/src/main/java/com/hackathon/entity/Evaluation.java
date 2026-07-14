package com.hackathon.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hackathon.entity.enums.EvaluationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
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
    @Enumerated(EnumType.STRING)
    private EvaluationStatus status;
    @Column(name="Original_Score", precision = 10 , scale = 2, nullable = true)
    private BigDecimal originalScore;
    @Column(name="Is_ReEvaluation")
    private Boolean isReEvaluation;

    //1 expertAsgin -N evaluation
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Expert_ID", nullable = false)
    private ExpertAssign expertAssign;

    // 1 Submission -N EVALUATION
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "Submission_ID", nullable = false)
    private Submission submission;

    // 1 Evaluation - N Evaluation Detail
    @OneToMany(mappedBy = "evaluation", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<EvaluationDetail> evaluationDetails= new ArrayList<>();

}
