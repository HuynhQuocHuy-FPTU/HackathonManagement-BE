package com.hackathon.entity;

import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name="Account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Account_ID")
    private int accountId;

    @Column(name = "Password", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String password;

    @Column(name = "Phone", columnDefinition = "VARCHAR(10)")
    private String phone;

    @Column(name = "Email", nullable = false, columnDefinition = "VARCHAR(255)", unique = true)
    private String email;

    @Column(name = "Status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus status;

    @Column(name = "Created_At", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "Avatar_Url", columnDefinition = "NVARCHAR(255)")
    private String avatarUrl;

    @Column(name = "Updated_At")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false)
    private AccountRole role;
    @Column(name = "Github_Id")
    private Long githubId;
    @Column(name = "Github_Username")
    private String githubUsername;
    @Column(length = 1000, name = "Github_Access_Token")
    private String githubAccessToken;

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

    @OneToMany(mappedBy = "actor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> sentNotifications;

    // 1 account - N Auditlog
    @OneToMany(
            mappedBy = "account",
            fetch = FetchType.LAZY)
    private List<AuditLog> auditLogs;

    @Column(name = "Verification_Token", columnDefinition = "VARCHAR(255)")
    private String verificationToken;

    @Column(name = "Verification_Token_Expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "Reset_Password_Otp", columnDefinition = "VARCHAR(6)")
    private String resetPasswordOtp;

    @Column(name = "Reset_Password_Otp_Expiry")
    private LocalDateTime resetPasswordOtpExpiry;

    @Column(name = "Is_Password_Changed", nullable = false)
    private boolean isPasswordChanged;
}
