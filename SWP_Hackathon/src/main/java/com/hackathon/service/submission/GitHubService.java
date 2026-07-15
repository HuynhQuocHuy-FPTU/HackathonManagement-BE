package com.hackathon.service.submission;

import com.hackathon.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
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
        System.out.println("Start getLatestCommitSha");
        if (repoUrl == null || repoUrl.isBlank()) {
            return null;
        }

        String repoFullName = extractRepoFullName(repoUrl);

        System.out.println("Calling GitHub API: " + repoUrl);
        try {
//            GitHub gitHub = GitHub.connectAnonymously();
            GHRepository repository = gitHub.getRepository(repoFullName);
            // Lấy nhánh mặc định
            String defaultBranch = repository.getDefaultBranch();
            GHBranch branch = repository.getBranch(defaultBranch);

            if (branch == null) {
                throw new IOException("Branch mặc định không tồn tại");
            }
            System.out.println("GitHub API returned");
            return branch.getSHA1();
        } catch (IOException e) {
            log.error("Lỗi khi lấy thông tin repository hoặc branch: {}", repoFullName, e);
            // Ném BadRequestException để hệ thống chấm bài nhận diện được lỗi nghiệp vụ
            throw new BadRequestException("Không thể truy xuất dữ liệu từ GitHub. Vui lòng kiểm tra lại URL!");
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
