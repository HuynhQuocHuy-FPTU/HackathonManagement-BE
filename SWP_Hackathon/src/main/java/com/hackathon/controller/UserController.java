package com.hackathon.controller;

import com.hackathon.dto.auth.AuthResponse;
import com.hackathon.dto.user.UpdateProfileRequest;
import com.hackathon.dto.user.UserProfileResponse;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Chưa đăng nhập"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Thông tin tài khoản", userService.getCurrentUser(userDetails)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Chưa đăng nhập"));
        }
        AuthResponse response = userService.updateProfile(userDetails, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin cá nhân thành công", response));
    }

    @GetMapping("/{accountId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfileById(
            @PathVariable("accountId") Integer accountId) {

        UserProfileResponse userProfile = userService.getUserProfileById(accountId);

        return ResponseEntity.ok(ApiResponse.ok("Tải hồ sơ người dùng thành công", userProfile));
    }

//    @PostMapping("/change-password")
//    public ResponseEntity<ApiResponse<Void>> changePassword() {
//        return ResponseEntity.ok(ApiResponse.ok("Tính năng đổi mật khẩu đang phát triển"));
//    }
//
//    @PostMapping("/avatar")
//    public ResponseEntity<ApiResponse<Void>> uploadAvatar() {
//        return ResponseEntity.ok(ApiResponse.ok("Tính năng upload ảnh đang phát triển"));
//    }
}