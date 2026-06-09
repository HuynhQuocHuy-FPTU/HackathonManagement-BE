package com.hackathon.entity;

import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name="Account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Account_ID")
    private int accountId;

    @Column(name = "Account_Name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String accountName;

    @Column(name = "Password", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String password;

    @Column(name = "Phone", nullable = false, columnDefinition = "VARCHAR(10)")
    private String phone;

    @Column(name = "Email", nullable = false, columnDefinition = "VARCHAR(255)", unique = true)
    private String email;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "Created_At", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "Updated_At", nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false)
    private AccountRole role;

    private  String avatarUrl;

    // 1 Account - 1 Expert
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private Expert expert;

    //1 Account - 1 Student
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private Student student;

    //1Account - 1 EventCoordinator
    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    private EventCoordinator eventCoordinator;
    //1 Account - N Notification
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> notifications;

    // 1 account - N Auditlog
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuditLog> auditLogs;
}
