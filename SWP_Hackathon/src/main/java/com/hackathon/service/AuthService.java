package com.hackathon.service;

import com.hackathon.dto.auth.ResetPasswordRequest;
import com.hackathon.dto.auth.LoginRequest;
import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.security.CustomUserDetails;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    AuthResponse refreshAccessToken(String refreshTokenValue);
    AuthResponse getCurrentUser(CustomUserDetails userDetails);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
}