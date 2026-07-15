package com.hackathon.dto.auth;

import com.hackathon.entity.enums.AccountRole;
import com.hackathon.entity.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private Long githubId;
    private String githubUsername;
    private long expiresIn;
    private int accountId;
    private String avatarUrl;
    private String fullName;
    private String email;
    private AccountRole role;
    private String university;
    private String organization;
    private LocalDateTime createdAt;
    private AccountStatus accountStatus;
    private boolean isPasswordChanged;
}