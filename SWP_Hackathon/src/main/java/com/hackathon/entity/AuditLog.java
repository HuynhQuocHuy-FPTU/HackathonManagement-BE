package com.hackathon.entity;

import com.hackathon.entity.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AuditAction action;
    @Column(columnDefinition = "NVARCHAR(255)")
    private String entityType;

    private Long entityId;

    @Column(columnDefinition = "NVARCHAR(1000)")
    private String description;
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // N Auditlog - 1 Account
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Account_Id")
    private Account account;
    @PrePersist
    public void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
