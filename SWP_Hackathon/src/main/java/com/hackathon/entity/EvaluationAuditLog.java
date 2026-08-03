package com.hackathon.entity;

import com.hackathon.entity.enums.EvaluationStatus;
import com.hackathon.entity.enums.CriteriaType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
        name = "Evaluation_Audit_Log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "UK_Evaluation_Audit_Attempt",
                        columnNames = {
                                "Evaluation_ID",
                                "Criteria_Type",
                                "Attempt_Number"})
        }
)
public class EvaluationAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Audit_Log_ID", nullable = false, unique = true)
    private AuditLog auditLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Evaluation_ID", nullable = false)
    private Evaluation evaluation;

    @Column(name = "Event_ID", nullable = false)
    private Integer eventId;

    @Column(name = "Round_ID", nullable = false)
    private Integer roundId;

    @Column(name = "Attempt_Number", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "Criteria_Type", nullable = false)
    private CriteriaType criteriaType;

    @Column(name = "Total_Score", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "Total_Comment", columnDefinition = "NVARCHAR(500)")
    private String totalComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "Evaluation_Status", nullable = false)
    private EvaluationStatus status;

    @OneToMany(mappedBy = "evaluationAuditLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluationDetailAuditLog> details =
            new ArrayList<>();

    @Column(name = "Created_At", nullable = false, updatable = false)
    private LocalDateTime createdAt;



}
