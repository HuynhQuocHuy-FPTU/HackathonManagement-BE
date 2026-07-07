package com.hackathon.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

//Response trả về từ https://github.com/login/oauth/access_token
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubTokenResponse {
    private String access_token;
    private String token_type;
    private String scope;
    private String error;
    private String error_description;
}
