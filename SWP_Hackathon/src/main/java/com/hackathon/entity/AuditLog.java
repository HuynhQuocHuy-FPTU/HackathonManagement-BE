package com.hackathon.entity;

import com.hackathon.entity.enums.AuditAction;
import com.hackathon.entity.enums.AuditEntityType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "Audit_Log",
        indexes = {
                @Index(
                        name = "idx_audit_account",
                        columnList = "Account_Id"
                ),
                @Index(
                        name = "idx_audit_created_at",
                        columnList = "createdAt"
                ),
                @Index(
                        name = "idx_audit_action",
                        columnList = "action"
                )
        }
)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    private AuditEntityType entityType;

    private Integer entityId;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // N Auditlog - 1 Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Account_Id", nullable = false)
    private Account account;
    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
