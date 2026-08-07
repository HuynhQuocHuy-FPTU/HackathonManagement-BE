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
                    throw new BadRequestException("Link github không hợp lệ! Vui lòng nhập đúng định dạng: http://github.com/username/repository-name");
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

        List<FileType> allowedFileTypes = round.getAllowedFileType();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new BadRequestException("File tải lên không được để trống.");
            }

            String fileName = file.getOriginalFilename();
            String extension = getExtension(fileName);
            FileType extensionType = FileType.fromExtension(extension);
            FileType mimeType = FileType.fromMimeType(file.getContentType());

            if (extensionType == null) {
                throw new BadRequestException(
                        "Loại file ." + (extension == null ? "không xác định" : extension)
                                + " không được phép nộp. File: " + fileName
                                + ". Các loại được phép: " + allowedFileTypes
                );
            }

            // Trình duyệt có thể không gửi MIME chuẩn cho ZIP/RAR, khi đó dùng phần mở rộng đã nhận diện.
            if (mimeType == null
                    && extensionType.getGroup() == FileType.FileGroup.ARCHIVE) {
                mimeType = extensionType;
            }

            if (mimeType == null) {
                throw new BadRequestException(
                        "Không nhận diện được MIME type của file: " + fileName
                                + ". MIME nhận được: " + file.getContentType()
                );
            }

            FileType resolvedMimeType = mimeType;

            if (!extensionType.getMimeType().equalsIgnoreCase(resolvedMimeType.getMimeType())) {
                throw new BadRequestException(
                        "Phần mở rộng không khớp với loại nội dung của file: " + fileName
                );
            }

            boolean isAllowed = allowedFileTypes == null
                    || allowedFileTypes.isEmpty()
                    || allowedFileTypes.stream().anyMatch(allowedType ->
                            allowedType.getMimeType().equalsIgnoreCase(resolvedMimeType.getMimeType())
                    );

            if (!isAllowed) {
                throw new BadRequestException(
                        "File " + fileName + " không thuộc loại được phép. Các loại được phép: "
                                + allowedFileTypes
                );
            }
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1);
    }


}
