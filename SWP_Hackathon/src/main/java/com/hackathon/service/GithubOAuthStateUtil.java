package com.hackathon.service;

public interface GithubOAuthStateUtil {

    String encode(Integer accountId);

    Integer decodeAndVerify(String state);
}
