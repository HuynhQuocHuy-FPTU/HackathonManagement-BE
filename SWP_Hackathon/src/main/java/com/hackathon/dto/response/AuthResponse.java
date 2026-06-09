package com.hackathon.dto.response;

import com.hackathon.entity.enums.AccountRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private int accountId;
    private String accountName;
    private String email;
    private AccountRole role;
}