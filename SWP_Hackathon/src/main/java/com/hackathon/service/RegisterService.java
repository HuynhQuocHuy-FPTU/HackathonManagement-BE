package com.hackathon.service;

import com.hackathon.dto.request.RegisterRequest;
import com.hackathon.dto.request.ResendVerificationRequest;

public interface RegisterService {
    void register(RegisterRequest request);
    void verifyEmail(String token);
    void resendVerification(ResendVerificationRequest request);
}