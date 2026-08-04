package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "Evaluation_Detail_Audit_Log")
@Getter
@Setter
@NoArgsConstructor
public class EvaluationDetailAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Evaluation_Audit_Log_ID", nullable = false
    )
    private EvaluationAuditLog evaluationAuditLog;

    @Column(name = "Evaluation_Detail_ID")
    private Integer evaluationDetailId;

    @Column(name = "Criteria_ID", nullable = true)
    private Integer criteriaId;

    @Column(name = "Criteria_Name", nullable = true, columnDefinition = "NVARCHAR(255)")
    private String criteriaName;

    @Column(name = "Score", nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "Comment", columnDefinition = "NVARCHAR(500)")
    private String comment;

    @Column(name = "Criteria_Weight", precision = 10, scale = 4)
    private BigDecimal criteriaWeight;
}
