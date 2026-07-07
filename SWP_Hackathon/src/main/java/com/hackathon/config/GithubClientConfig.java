package com.hackathon.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GithubClientConfig {

    @Value("${github.api.base-url:https://api.github.com}")
    private String baseUrl;

    @Value("${github.api.token}")
    private String githubToken;

    @Bean
    public GitHub gitHub() throws IOException {
        if (githubToken == null || githubToken.isEmpty()) {
            throw new IllegalStateException("GitHub System Token chưa được cấu hình!");
        }
        return new GitHubBuilder()
                .withEndpoint(baseUrl)
                .withOAuthToken(githubToken)
                .build();
    }
    public GitHub createUserClient(String accessToken) throws IOException{
        return new GitHubBuilder().withEndpoint(baseUrl).withOAuthToken(accessToken).build();
    }
}