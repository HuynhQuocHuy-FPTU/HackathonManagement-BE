package com.hackathon.service;

import com.hackathon.dto.auth.ChangePasswordRequest;
import com.hackathon.dto.auth.LoginRequest;
import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.auth.ResetPasswordRequest;
import com.hackathon.security.CustomUserDetails;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void logout(String refreshToken);
    AuthResponse refreshAccessToken(String refreshTokenValue);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(CustomUserDetails userDetails, ChangePasswordRequest request);
    AuthResponse loginWithGoogle(String email);
    void registerWithGoogle(String email);

}
