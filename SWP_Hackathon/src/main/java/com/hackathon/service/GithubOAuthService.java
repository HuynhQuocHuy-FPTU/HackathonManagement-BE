package com.hackathon.service;

import com.hackathon.security.CustomUserDetails;

public interface GithubOAuthService {

    String buildAuthorizeUrl(CustomUserDetails userDetails);

    void handleCallback(String code, String state);

    String fetchRepoOwnerLogin(String gitHubUrl);
}
