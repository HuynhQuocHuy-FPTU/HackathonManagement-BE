package com.hackathon.controller;

import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.GithubOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class GithubOAuthController {

    private final GithubOAuthService githubOAuthService;

    // Lấy URL của frontend từ application.properties
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Trả về URL để frontend redirect browser sang GitHub
    @GetMapping("/api/github/oauth/authorize")
    public ResponseEntity<String> authorize(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String authorizeUrl = githubOAuthService.buildAuthorizeUrl(userDetails);
        System.out.println("Authorize URL: " + authorizeUrl);
        return ResponseEntity.ok(authorizeUrl);
    }

    // GitHub redirect trở lại endpoint này sau khi người dùng cấp quyền.
    // Endpoint này KHÔNG yêu cầu Authentication (browser tự redirect, không mang
    // JWT)
    @GetMapping("/api/github/oauth/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        String redirect;
        try {
            githubOAuthService.handleCallback(code, state);
            // Redirect về trang profile của frontend khi thành công
            redirect = frontendUrl + "/profile?githubLinked=true";
        } catch (Exception e) {
            // Redirect về frontend kèm lỗi thay vì trả JSON thô, vì đây là request
            // browser tự chuyển hướng tới (không phải gọi API từ SPA).
            redirect = frontendUrl + "/profile?githubLinked=false&error="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect))
                .build();
    }
}
