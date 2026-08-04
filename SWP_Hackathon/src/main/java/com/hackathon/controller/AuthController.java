package com.hackathon.controller;

import com.hackathon.dto.auth.*;
import com.hackathon.exception.ApiResponse;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.impl.StudentServiceImpl;
import com.hackathon.service.AuthService;
import com.hackathon.service.RegisterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/account")
public class AuthController {

    private final AuthService authService;
    private final RegisterService registerService; // Thêm RegisterService
    private final StudentServiceImpl studentService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        // Gọi sang registerService thay vì authService
        registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản."));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        // Gọi sang registerService
        registerService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.ok("Xác thực email thành công. Bạn có thể đăng nhập."));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        // Gọi sang registerService
        registerService.resendVerification(request);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi lại email xác thực."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Giữ nguyên dùng authService
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Làm mới token thành công", response));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Nếu email tồn tại trong hệ thống, mã OTP đã được gửi đến email của bạn."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới."));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userDetails, request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công! Vui lòng đăng nhập lại."));
    }

    @PostMapping("/register-google")
    public ResponseEntity<ApiResponse<Void>> registerWithGoogle(@RequestParam String email) {
        authService.registerWithGoogle(email);
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký thành công"));
    }

    @GetMapping("/oauth2/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/google");
    }
    @PostMapping("/complete-register")
    public ResponseEntity<ApiResponse<Void>>completeRegister(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid
            @RequestBody StudentUpdateRequest request) {
        studentService.completeRegister(userDetails, request);
        return ResponseEntity.ok(ApiResponse.ok("Hoàn tất đăng ký"));
    }

}
