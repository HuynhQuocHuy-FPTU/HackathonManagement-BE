package com.hackathon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "RefreshToken")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Refresh_Token_ID")
    private Long id;

    @Column(name = "Token", nullable = false, unique = true, columnDefinition = "VARCHAR(512)")
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "Account_ID", nullable = false)
    private Account account;

    @Column(name = "Expiry_Date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "Revoked", nullable = false)
    private boolean revoked = false;
}
