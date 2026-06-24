package com.hackathon.service.user;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.user.UpdateProfileRequest;
import com.hackathon.security.CustomUserDetails;

public interface UserService {
    AuthResponse getCurrentUser(CustomUserDetails userDetails);
    AuthResponse updateProfile(CustomUserDetails userDetails, UpdateProfileRequest request);
}