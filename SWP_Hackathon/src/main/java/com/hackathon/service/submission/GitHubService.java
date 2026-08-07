package com.hackathon.service.submission;

import com.hackathon.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.HttpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitHubService {
    private static final String GITHUB_REGEX = "^https://github\\.com/[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+$";
    private static final Pattern PATTERN = Pattern.compile(GITHUB_REGEX);
    private final GitHub gitHub;

    public boolean isValidGithubUrl(String url){
        if(url == null || url.isBlank()) return false;
        return PATTERN.matcher(url).matches();
    }

    //Lấy lần commit cuối cùng
    public String getLatestCommitSha(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return null;
        }

        String repoFullName = extractRepoFullName(repoUrl);
        System.out.println("Calling GitHub API: " + repoUrl);

        try {
            return loadLatestCommitSha(gitHub, repoFullName);
        } catch (HttpException e) {
            if (e.getResponseCode() == 409) {
                return null;
            }
            log.warn("GitHub token hệ thống trả mã lỗi {}, thử truy cập repository công khai", e.getResponseCode());
            return loadPublicRepository(repoFullName);
        } catch (IOException e) {
            log.warn("Không thể truy cập GitHub bằng token hệ thống, thử truy cập repository công khai", e);
            return loadPublicRepository(repoFullName);
        }
    }

    // Dùng truy cập công khai làm phương án dự phòng khi token hệ thống sai hoặc hết hạn.
    private String loadPublicRepository(String repoFullName) {
        try {
            return loadLatestCommitSha(GitHub.connectAnonymously(), repoFullName);
        } catch (HttpException e) {
            if (e.getResponseCode() == 409) {
                return null;
            }
            log.error("GitHub API công khai trả mã lỗi {} cho repository {}", e.getResponseCode(), repoFullName, e);
            throw new BadRequestException("Không thể truy xuất dữ liệu từ GitHub. Vui lòng kiểm tra lại URL!");
        } catch (IOException e) {
            log.error("Không thể truy cập repository công khai: {}", repoFullName, e);
            throw new BadRequestException("Không thể truy xuất dữ liệu từ GitHub. Vui lòng kiểm tra lại URL!");
        }
    }

    private String loadLatestCommitSha(GitHub client, String repoFullName) throws IOException {
        GHRepository repository = client.getRepository(repoFullName);

        if (repository.getPushedAt() == null) {
            log.info("Repository {} chưa có commit", repoFullName);
            return null;
        }

        String defaultBranch = repository.getDefaultBranch();
        if (defaultBranch == null || defaultBranch.isBlank()) {
            return null;
        }

        try {
            GHBranch branch = repository.getBranch(defaultBranch);
            return branch != null ? branch.getSHA1() : null;
        } catch (GHFileNotFoundException e) {
            if (repository.getSize() == 0) {
                return null;
            }
            throw e;
        }
    }

    public void verifyCommitExistence(String commitUrl) {
        Matcher matcher = PATTERN.matcher(commitUrl);
        if (!matcher.matches()) throw new BadRequestException("Link không hợp lệ!");

        String repoFullName = matcher.group(1);
        String sha = matcher.group(2);

        try {
            GitHub gitHub = GitHub.connectAnonymously();
            gitHub.getRepository(repoFullName).getCommit(sha);
        } catch (IOException e) {
            log.error("Commit không tồn tại: {}", sha);
            // Ném lỗi để logic chấm bài biết và gán 0 điểm
            throw new BadRequestException("Commit không tồn tại trên GitHub!");
        }
    }

    private String extractRepoFullName(String url) {
        // Loại bỏ "https://github.com/" để lấy "username/repo"
        return url.replace("https://github.com/", "");
    }
}
