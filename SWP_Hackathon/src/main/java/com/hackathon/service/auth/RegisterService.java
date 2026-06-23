package com.hackathon.service.auth;

import com.hackathon.dto.auth.RegisterRequest;
import com.hackathon.dto.auth.ResendVerificationRequest;

public interface RegisterService {
    void register(RegisterRequest request);
    void verifyEmail(String token);
    void resendVerification(ResendVerificationRequest request);
}