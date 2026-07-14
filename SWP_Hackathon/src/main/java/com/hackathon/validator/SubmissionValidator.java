package com.hackathon.validator;

import com.hackathon.entity.Round;
import com.hackathon.entity.enums.FileType;
import com.hackathon.entity.enums.RoundStatus;
import com.hackathon.exception.ApiException;
import com.hackathon.exception.BadRequestException;
import com.hackathon.service.submission.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SubmissionValidator {
    private final GitHubService gitHubService;
    public void validateSubmission(Round round, List<MultipartFile> files, String githubUrl) {
        // 1. Kiểm tra deadline
        if (round.getSubmissionDeadline() != null && LocalDateTime.now().isAfter(round.getSubmissionDeadline())) {
            throw new BadRequestException("Đã hết thời gian nộp bài cho vòng thi này!");
        }
        if (round.getStatus() == RoundStatus.UPCOMING) {
            throw new BadRequestException("Vòng thi chưa bắt đầu, bạn chưa thể nộp bài!");
        }else if (round.getStatus() == RoundStatus.EVALUATING){
            throw new BadRequestException("Đã hết thời gian nộp bài cho vòng thi này!");
        }

        // 2. Kiểm tra yêu cầu nộp (GitHub, File, hoặc cả hai)
        switch (round.getSubmissionType()) {
            case GITHUB_URL:
                if (githubUrl == null || githubUrl.isBlank()) throw new BadRequestException("Vòng này yêu cầu nộp link GitHub!");
                if(!gitHubService.isValidGithubUrl(githubUrl)){
                    throw new BadRequestException("Link github không hợp lệ! Vui lòng nhập đúng định dạng: https://github.com/username/repository-name");
                }
                break;
            case FILE:
                if (files == null || files.isEmpty()) throw new BadRequestException("Vòng này yêu cầu nộp file!");
                validateFiles(files, round);
                break;
            case BOTH:
                if ((githubUrl == null || githubUrl.isBlank()) || (files == null || files.isEmpty()))
                    throw new BadRequestException("Vòng này yêu cầu nộp cả GitHub và File!");
                if(!gitHubService.isValidGithubUrl(githubUrl)){
                    throw new BadRequestException("Link github không hợp lệ! Vui lòng nhập đúng định dạng: https://github.com/username/repository-name");
                }
                validateFiles(files, round);
                break;
        }

    }
    private void validateFiles(List<MultipartFile> files, Round round) {
        // 1. Kiểm tra số lượng file
        if (round.getMaxFileCount() != null && files.size() > round.getMaxFileCount()) {
            throw new BadRequestException("Mỗi lần nộp không được vượt quá " + round.getMaxFileCount() + " file.");
        }

        // 2. Kiểm tra từng file
        long totalSize = 0;
        for (MultipartFile file : files) {
            validateFile(file, round.getAllowedFileType(), round.getMaxTotalSizeMb());
            totalSize += file.getSize();
        }

        // 3. Kiểm tra tổng dung lượng
        if (round.getMaxTotalSizeMb() != null && (totalSize / (1024 * 1024)) > round.getMaxTotalSizeMb()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "Tổng dung lượng các file vượt quá giới hạn " + round.getMaxTotalSizeMb() + "MB");
        }
    }


    private void validateFile(MultipartFile file, List<FileType> allowedTypes, Integer maxSizeMb) {
        // Kiểm tra loại file
        String fileName = file.getOriginalFilename(); // Phải dùng getOriginalFilename() thay vì getName()
        String extension = (fileName != null && fileName.contains("."))
                ? fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase() : "";

        boolean isAllowed = allowedTypes.stream().anyMatch(type -> type.name().equalsIgnoreCase(extension));
        if (!isAllowed) {
            throw new BadRequestException("Loại file không được hỗ trợ: " + extension +
                    ". Các loại file được phép là: " + allowedTypes);
        }

        // Kiểm tra dung lượng file lẻ
        long fileSizeInMB = file.getSize() / (1024 * 1024);
        if (fileSizeInMB > maxSizeMb) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File " + fileName + " vượt quá giới hạn " + maxSizeMb + "MB");
        }
    }
}
