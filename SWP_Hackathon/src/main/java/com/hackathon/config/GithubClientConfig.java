package com.hackathon.config;

import io.jsonwebtoken.io.IOException;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GithubClientConfig {
    @Value("${github.api.base-url:https://api.github.com}")
    private String baseUrl;

    @Value("${github.api.token}")
    private String githubToken;

    @Bean
    public GitHub gitHub() throws IOException, java.io.IOException {
        return new GitHubBuilder()
                .withEndpoint(baseUrl)
                .withOAuthToken(githubToken)
                .build();
    }
}
