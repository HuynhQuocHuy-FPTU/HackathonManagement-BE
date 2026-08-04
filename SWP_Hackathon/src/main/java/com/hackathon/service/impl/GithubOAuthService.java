package com.hackathon.service.impl;

import com.hackathon.dto.github.GithubRepoResponse;
import com.hackathon.dto.github.GithubTokenResponse;
import com.hackathon.dto.github.GithubUserInfoResponse;
import com.hackathon.entity.Account;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubOAuthService {

    private final AccountRepository accountRepository;
    private final GithubOAuthStateUtil stateUtil;

    @Value("${github.client-id}")
    private String clientId;

    @Value("${github.client-secret}")
    private String clientSecret;

    @Value("${github.redirect-uri}")
    private String redirectUri;

    private final RestClient restClient = RestClient.create();

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^https?://github\\.com/([A-Za-z0-9-]+)/([A-Za-z0-9._-]+?)(?:\\.git)?/?$"
    );

    // Bước 1: Tạo URL để frontend redirect người dùng sang trang GitHub xin quyền.
     //accountId của người đang đăng nhập được ký vào "state" để xác minh ở bước callback.

    public String buildAuthorizeUrl(CustomUserDetails userDetails) {
        if(userDetails == null){
            throw new BadRequestException("Bạn cần đăng nhập trước khi liên kết tài khoản Github");
        }
        Integer accountId = userDetails.getAccount().getAccountId();
        String state = stateUtil.encode(accountId);

        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user user:email")
                .queryParam("state", state)
                .queryParam("allow_signup", "false")
                .build()
                .toUriString();
    }

    //Bước 2: Xử lý callback từ GitHub — đổi code lấy access_token, lấy thông tin user
    @Transactional
    public void handleCallback(String code, String state) {
        Integer accountId;
        try {
            accountId = stateUtil.decodeAndVerify(state);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        String accessToken = exchangeCodeForToken(code);
        GithubUserInfoResponse githubUser = fetchGithubUser(accessToken);

        // Kiểm tra tài khoản GitHub này đã được liên kết với account khác chưa
        accountRepository.findAccountByGithubId(githubUser.getId())
                .filter(existing -> existing.getAccountId() != account.getAccountId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Tài khoản GitHub này đã được liên kết với một account khác");
                });

        account.setGithubId(githubUser.getId());
        account.setGithubUsername(githubUser.getLogin());
        account.setGithubAccessToken(accessToken);
        accountRepository.save(account);

        log.info("Account {} đã liên kết GitHub username {}", account.getAccountId(), githubUser.getLogin());
    }

    private String exchangeCodeForToken(String code) {
        String body = "client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(redirectUri);

        GithubTokenResponse tokenResponse = restClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(GithubTokenResponse.class);

        if (tokenResponse == null || tokenResponse.getAccess_token() == null) {
            String detail = tokenResponse != null ? tokenResponse.getError_description() : "không có phản hồi";
            throw new BadRequestException("Không thể liên kết GitHub: " + detail);
        }

        return tokenResponse.getAccess_token();
    }

    private GithubUserInfoResponse fetchGithubUser(String accessToken) {
        return restClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(GithubUserInfoResponse.class);
    }


      //Dùng khi nộp bài: lấy owner login chính xác từ GitHub API (xử lý cả trường hợp
     //public repo bị đổi tên / redirect), không cần access token vì chỉ áp dụng cho repo public.
      //Trả về null nếu repo không tồn tại hoặc không truy cập được (coi như không xác minh được)

    public String fetchRepoOwnerLogin(String gitHubUrl) {
        Matcher matcher = GITHUB_URL_PATTERN.matcher(gitHubUrl.trim());
        if (!matcher.matches()) {
            throw new BadRequestException("Đường dẫn GitHub không hợp lệ");
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);

        try {
            GithubRepoResponse repoResponse = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}", owner, repo)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                        throw new BadRequestException("Không tìm thấy repo GitHub này (repo không tồn tại hoặc là private)");
                    })
                    .body(GithubRepoResponse.class);

            return repoResponse != null && repoResponse.getOwner() != null
                    ? repoResponse.getOwner().getLogin()
                    : owner; // fallback: dùng owner parse từ URL nếu response thiếu field
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Không thể gọi GitHub API để xác minh repo {}/{}: {}", owner, repo, e.getMessage());
            // Không chặn nộp bài chỉ vì GitHub API tạm thời lỗi/rate-limit;
            // fallback về owner parse trực tiếp từ URL.
            return owner;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
