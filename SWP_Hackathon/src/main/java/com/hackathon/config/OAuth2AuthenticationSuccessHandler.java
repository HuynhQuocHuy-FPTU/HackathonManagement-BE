package com.hackathon.config;

import com.hackathon.dto.auth.AuthResponse;

import com.hackathon.repository.AccountRepository;
import com.hackathon.service.auth.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final AccountRepository accountRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
//        String action = customAuthorizationRequestRepository.getAction(request);

        boolean exists = accountRepository.existsByEmail(email.trim());
        // Chưa có tài khoản -> đăng ký Google
        if (!exists) {
            authService.registerWithGoogle(email);
            AuthResponse authResponse = authService.loginWithGoogle(email);
//            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
//            response.sendRedirect("http://localhost:3000/complete-registration?email=" + encodedEmail);
//            return;
            response.sendRedirect(
                    "http://localhost:3000/complete-registration"
                            + "?accessToken="
                            + authResponse.getAccessToken()
                            + "&refreshToken="
                            + authResponse.getRefreshToken()
            );

            return;
        }


        // 1. Tạo JWT Token từ email dựa trên logic JwtService hiện tại của bạn
        AuthResponse authResponse = authService.loginWithGoogle(email);
        // 2. Chuyển hướng (Redirect) về giao diện Front-end kèm theo Token trên URL để Front-end lưu lại

        String accessToken = URLEncoder.encode(authResponse.getAccessToken(), StandardCharsets.UTF_8);
        String refreshToken = URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8);
        response.sendRedirect(
                "http://localhost:3000/oauth2/redirect?accessToken="
                        + accessToken
                        + "&refreshToken="
                        + refreshToken
        );
    }
}
