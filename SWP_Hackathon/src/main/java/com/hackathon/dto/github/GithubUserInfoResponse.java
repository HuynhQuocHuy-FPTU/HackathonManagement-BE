package com.hackathon.dto.github;
//Response trả về từ https://api.github.com/user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubUserInfoResponse {
    private Long id;
    private String login;
    private String name;
    private String avatar_url;
    private String html_url;
    private String email;
}
