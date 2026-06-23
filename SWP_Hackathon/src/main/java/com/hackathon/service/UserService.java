package com.hackathon.service;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.auth.UpdateProfileRequest;
import com.hackathon.security.CustomUserDetails;

public interface UserService {
    AuthResponse getCurrentUser(CustomUserDetails userDetails);
    AuthResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request);
}