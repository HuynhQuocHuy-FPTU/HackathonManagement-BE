package com.hackathon.service;

import com.hackathon.dto.request.LoginRequest;
import com.hackathon.dto.response.AuthResponse;
import com.hackathon.security.CustomUserDetails;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    AuthResponse refreshAccessToken(String refreshTokenValue);
    AuthResponse getCurrentUser(CustomUserDetails userDetails);
}