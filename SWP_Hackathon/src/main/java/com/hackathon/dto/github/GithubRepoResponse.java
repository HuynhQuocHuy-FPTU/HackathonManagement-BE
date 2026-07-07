package com.hackathon.dto.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

//Response (rút gọn) trả về từ https://api.github.com/repos/{owner}/{repo}
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoResponse {
    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("private")
    private Boolean isPrivate; // "private" là từ khoá Java nên phải map qua @JsonProperty

    private RepoOwner owner;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RepoOwner {
        private String login;
    }
}
