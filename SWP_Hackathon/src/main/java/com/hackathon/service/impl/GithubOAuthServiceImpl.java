package com.hackathon.service.impl;

import com.hackathon.dto.github.GithubRepoResponse;
import com.hackathon.dto.github.GithubTokenResponse;
import com.hackathon.dto.github.GithubUserInfoResponse;
import com.hackathon.entity.Account;
import com.hackathon.exception.BadRequestException;
import com.hackathon.exception.ResourceNotFoundException;
import com.hackathon.repository.AccountRepository;
import com.hackathon.security.CustomUserDetails;
import com.hackathon.service.GithubOAuthService;
import com.hackathon.service.GithubOAuthStateUtil;
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
// Liên kết tài khoản hệ thống với GitHub và xác minh chủ sở hữu kho mã nguồn khi nộp bài.
public class GithubOAuthServiceImpl implements GithubOAuthService {

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
            "^https?://github\\.com/([A-Za-z0-9-]+)/([A-Za-z0-9._-]+?)(?:\\.git)?/?$");

    // Tạo đường dẫn chuyển người dùng sang GitHub để cấp quyền liên kết tài khoản.
    @Override
    public String buildAuthorizeUrl(CustomUserDetails userDetails) {
        // Người dùng phải đăng nhập để trạng thái liên kết gắn với đúng tài khoản.
        if (userDetails == null) {
            throw new BadRequestException("Bạn cần đăng nhập trước khi liên kết tài khoản Github");
        }
        // Lấy mã tài khoản sẽ nhận thông tin GitHub sau khi liên kết thành công.
        Integer accountId = userDetails.getAccount().getAccountId();
        // Ký mã tài khoản vào trạng thái để chống giả mạo yêu cầu trả về.
        String state = stateUtil.encode(accountId);

        // Ghép các tham số ứng dụng, quyền truy cập và trạng thái vào đường dẫn cấp quyền.
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "read:user user:email")
                .queryParam("state", state)
                .queryParam("allow_signup", "false")
                .build()
                .toUriString();
    }

    // Xử lý dữ liệu GitHub trả về, xác minh thư điện tử và lưu thông tin liên kết.
    @Transactional
    @Override
    public void handleCallback(String code, String state) {
        // Giải mã trạng thái để xác định tài khoản đã bắt đầu yêu cầu liên kết.
        Integer accountId;
        try {
            accountId = stateUtil.decodeAndVerify(state);
            // Chuyển lỗi trạng thái hết hạn hoặc giả mạo thành lỗi yêu cầu cho phía gọi.
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        // Tải tài khoản cần liên kết từ mã đã được xác minh trong trạng thái.
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        // Đổi mã tạm thời do GitHub cấp thành mã truy cập.
        String accessToken = exchangeCodeForToken(code);
        // Dùng mã truy cập để lấy hồ sơ GitHub của người vừa cấp quyền.
        GithubUserInfoResponse githubUser = fetchGithubUser(accessToken);

        // Theo dõi kết quả đối chiếu thư điện tử giữa GitHub và tài khoản hệ thống.
        boolean emailMatched = false;
        // Ưu tiên thư điện tử công khai trong hồ sơ GitHub.
        String githubEmail = githubUser.getEmail();

        // Chấp nhận ngay khi thư điện tử công khai trùng với tài khoản hiện tại.
        if (githubEmail != null && githubEmail.equalsIgnoreCase(account.getEmail())) {
            emailMatched = true;
        } else {
            // Nếu thư công khai bị ẩn hoặc không khớp, lấy danh sách thư đã xác minh từ GitHub.
            try {
                java.util.List<java.util.Map<String, Object>> emails = restClient.get()
                        .uri("https://api.github.com/user/emails")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                        .retrieve()
                        .body(new org.springframework.core.ParameterizedTypeReference<java.util.List<java.util.Map<String, Object>>>() {});

                // Duyệt danh sách để tìm đúng thư đã được GitHub xác minh.
                if (emails != null) {
                    for (java.util.Map<String, Object> emailObj : emails) {
                        String emailStr = (String) emailObj.get("email");
                        Boolean verified = (Boolean) emailObj.get("verified");
                        // Chỉ chấp nhận thư đã xác minh và trùng với tài khoản hệ thống.
                        if (Boolean.TRUE.equals(verified) && account.getEmail().equalsIgnoreCase(emailStr)) {
                            emailMatched = true;
                            githubEmail = emailStr; // Ghi nhận email khớp
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Không thể lấy danh sách email từ GitHub: {}", e.getMessage());
            }
        }

            // Từ chối liên kết nếu không chứng minh được hai tài khoản dùng cùng thư điện tử.
        if (!emailMatched) {
            githubEmail = githubEmail == null ? "Bị ẩn/Không có" : githubEmail;
            throw new BadRequestException("Email không chính chủ: Email GitHub (" + githubEmail + ") không khớp với email tài khoản.");
        }

        // Ngăn một tài khoản GitHub được liên kết đồng thời với nhiều tài khoản hệ thống.
        accountRepository.findAccountByGithubId(githubUser.getId())
                .filter(existing -> existing.getAccountId() != account.getAccountId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Tài khoản GitHub này đã được liên kết với một account khác");
                });

        // Lưu mã định danh, tên người dùng và mã truy cập GitHub vào tài khoản hiện tại.
        account.setGithubId(githubUser.getId());
        account.setGithubUsername(githubUser.getLogin());
        account.setGithubAccessToken(accessToken);
        // Ghi các thay đổi liên kết xuống cơ sở dữ liệu.
        accountRepository.save(account);

        log.info("Account {} đã liên kết GitHub username {}", account.getAccountId(), githubUser.getLogin());
    }

    // Đổi mã tạm thời nhận từ GitHub thành mã truy cập có thể gọi giao diện lập trình.
    private String exchangeCodeForToken(String code) {
        // Tạo nội dung biểu mẫu và mã hóa từng giá trị trước khi gửi.
        String body = "client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&code=" + urlEncode(code)
                + "&redirect_uri=" + urlEncode(redirectUri);

        // Gửi yêu cầu đổi mã và đọc phản hồi dưới dạng thông tin mã truy cập.
        GithubTokenResponse tokenResponse = restClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(GithubTokenResponse.class);

        // Phản hồi rỗng hoặc thiếu mã truy cập được xem là liên kết thất bại.
        if (tokenResponse == null || tokenResponse.getAccess_token() == null) {
            String detail = tokenResponse != null ? tokenResponse.getError_description() : "không có phản hồi";
            throw new BadRequestException("Không thể liên kết GitHub: " + detail);
        }

        // Trả mã truy cập hợp lệ cho bước lấy hồ sơ GitHub.
        return tokenResponse.getAccess_token();
    }

    // Lấy hồ sơ GitHub của người dùng đang sở hữu mã truy cập.
    private GithubUserInfoResponse fetchGithubUser(String accessToken) {
        return restClient.get()
                .uri("https://api.github.com/user")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .retrieve()
                .body(GithubUserInfoResponse.class);
    }

    // Xác minh đường dẫn kho công khai và lấy chính xác tên chủ sở hữu từ GitHub.
    @Override
    public String fetchRepoOwnerLogin(String gitHubUrl) {
        // So khớp cấu trúc đường dẫn trước khi tách chủ sở hữu và tên kho.
        Matcher matcher = GITHUB_URL_PATTERN.matcher(gitHubUrl.trim());
        if (!matcher.matches()) {
            throw new BadRequestException("Đường dẫn GitHub không hợp lệ");
        }

        // Lấy chủ sở hữu và tên kho từ các nhóm trong biểu thức chính quy.
        String owner = matcher.group(1);
        String repo = matcher.group(2);

        try {
            // Gọi GitHub để xác nhận kho tồn tại và đọc thông tin chủ sở hữu chuẩn hóa.
            GithubRepoResponse repoResponse = restClient.get()
                    .uri("https://api.github.com/repos/{owner}/{repo}", owner, repo)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (req, res) -> {
                        throw new BadRequestException(
                                "Không tìm thấy repo GitHub này (repo không tồn tại hoặc là private)");
                    })
                    .body(GithubRepoResponse.class);

            // Ưu tiên tên chủ sở hữu trong phản hồi, nếu thiếu thì dùng giá trị tách từ đường dẫn.
            return repoResponse != null && repoResponse.getOwner() != null
                    ? repoResponse.getOwner().getLogin()
                    : owner; // fallback: dùng owner parse từ URL nếu response thiếu field
            // Giữ nguyên lỗi nghiệp vụ đã được tạo khi kho không tồn tại hoặc không công khai.
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Không thể gọi GitHub API để xác minh repo {}/{}: {}", owner, repo, e.getMessage());
            // Không chặn nộp bài khi GitHub tạm thời lỗi hoặc giới hạn số lần gọi.
            // Trong trường hợp đó dùng tên chủ sở hữu đã tách trực tiếp từ đường dẫn.
            return owner;
        }
    }

    // Mã hóa một giá trị để có thể đặt an toàn trong nội dung biểu mẫu gửi qua mạng.
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
